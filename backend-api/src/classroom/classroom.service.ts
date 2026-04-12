import { Injectable, Logger, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { OAuth2Client } from 'google-auth-library';
import { GoogleTokenService } from './services/google-token.service';
import { ClassroomCourseDto } from './dto/classroom-course.dto';
import { ClassroomTaskDto } from './dto/classroom-task.dto';

const CLASSROOM_API = 'https://classroom.googleapis.com/v1';

/**
 * Duración del cache en milisegundos. 5 minutos es un buen balance: el
 * usuario no necesita datos de Classroom actualizados al segundo, pero sí
 * que reflejen cambios razonablemente rápido.
 */
const CACHE_TTL_MS = 5 * 60 * 1000;

interface CacheEntry<T> {
  data: T;
  timestamp: number;
}

@Injectable()
export class ClassroomService {
  private readonly logger = new Logger(ClassroomService.name);
  private readonly oauth2Client: OAuth2Client;

  /**
   * Cache in-memory por userId. Se invalida automáticamente por TTL.
   * En un despliegue multi-instancia habría que usar Redis, pero para
   * la escala actual (un usuario por vez) basta con un Map en memoria.
   */
  private readonly coursesCache = new Map<
    number,
    CacheEntry<ClassroomCourseDto[]>
  >();
  private readonly allTasksCache = new Map<
    number,
    CacheEntry<ClassroomTaskDto[]>
  >();

  constructor(
    private readonly googleTokenService: GoogleTokenService,
    private readonly configService: ConfigService,
  ) {
    this.oauth2Client = new OAuth2Client(
      this.configService.get<string>('GOOGLE_CLIENT_ID'),
      this.configService.get<string>('GOOGLE_CLIENT_SECRET'),
    );
  }

  private isCacheValid<T>(entry: CacheEntry<T> | undefined): boolean {
    if (!entry) return false;
    return Date.now() - entry.timestamp < CACHE_TTL_MS;
  }

  private async authenticatedRequest<T>(
    userId: number,
    url: string,
    params?: Record<string, string>,
  ): Promise<T> {
    const accessToken =
      await this.googleTokenService.getValidAccessToken(userId);
    this.oauth2Client.setCredentials({ access_token: accessToken });

    try {
      const queryString = params
        ? '?' + new URLSearchParams(params).toString()
        : '';
      const response = await this.oauth2Client.request<T>({
        url: `${url}${queryString}`,
      });
      return response.data;
    } catch (error: unknown) {
      if (
        error &&
        typeof error === 'object' &&
        'response' in error &&
        (error as { response?: { status?: number } }).response?.status === 401
      ) {
        throw new UnauthorizedException(
          'Google Classroom token revoked. Please re-authorize.',
        );
      }
      throw error;
    }
  }

  async getCourses(userId: number): Promise<ClassroomCourseDto[]> {
    // Cache hit?
    const cached = this.coursesCache.get(userId);
    if (this.isCacheValid(cached)) {
      this.logger.debug(
        `getCourses(${userId}) — cache hit (${cached!.data.length} courses)`,
      );
      return cached!.data;
    }

    const data = await this.authenticatedRequest<{
      courses?: Array<{
        id: string;
        name: string;
        section?: string;
        descriptionHeading?: string;
        courseState: string;
        alternateLink: string;
      }>;
    }>(userId, `${CLASSROOM_API}/courses`, {
      courseStates: 'ACTIVE',
      studentId: 'me',
    });

    const courses =
      data.courses?.map((c) => ({
        id: c.id,
        name: c.name,
        section: c.section,
        descriptionHeading: c.descriptionHeading,
        courseState: c.courseState,
        alternateLink: c.alternateLink,
      })) ?? [];

    this.coursesCache.set(userId, { data: courses, timestamp: Date.now() });
    this.logger.log(
      `getCourses(${userId}) — fetched ${courses.length} courses from Google`,
    );
    return courses;
  }

  async getTasksByCourse(
    userId: number,
    courseId: string,
    courseName?: string,
  ): Promise<ClassroomTaskDto[]> {
    const courseWorkData = await this.authenticatedRequest<{
      courseWork?: Array<{
        id: string;
        title: string;
        description?: string;
        dueDate?: { year: number; month: number; day: number };
        dueTime?: { hours?: number; minutes?: number };
        maxPoints?: number;
        alternateLink?: string;
      }>;
    }>(userId, `${CLASSROOM_API}/courses/${courseId}/courseWork`);

    const courseWorkItems = courseWorkData.courseWork ?? [];

    const tasks: ClassroomTaskDto[] = [];

    for (const cw of courseWorkItems) {
      let submissionState = 'NEW';
      try {
        const submData = await this.authenticatedRequest<{
          studentSubmissions?: Array<{ state: string }>;
        }>(
          userId,
          `${CLASSROOM_API}/courses/${courseId}/courseWork/${cw.id}/studentSubmissions`,
          { userId: 'me' },
        );
        if (submData.studentSubmissions?.length) {
          submissionState = submData.studentSubmissions[0].state;
        }
      } catch {}

      let dueDate: string | undefined;
      if (cw.dueDate) {
        const { year, month, day } = cw.dueDate;
        const hours = cw.dueTime?.hours ?? 23;
        const minutes = cw.dueTime?.minutes ?? 59;
        dueDate = new Date(year, month - 1, day, hours, minutes).toISOString();
      }

      tasks.push({
        id: cw.id,
        courseId,
        courseName: courseName ?? '',
        title: cw.title,
        description: cw.description,
        dueDate,
        submissionState,
        alternateLink: cw.alternateLink,
        maxPoints: cw.maxPoints,
      });
    }

    return tasks;
  }

  /**
   * Devuelve todas las tasks de todos los cursos. El resultado se cachea
   * durante CACHE_TTL_MS (5 minutos). Esto evita pegar a la API de Google
   * Classroom en cada request del Android (Dashboard, Kanban, sync, etc.)
   * y resuelve el 429 RESOURCE_EXHAUSTED que aparecía con el flujo
   * anterior sin cache.
   */
  async getAllTasks(userId: number): Promise<ClassroomTaskDto[]> {
    const cached = this.allTasksCache.get(userId);
    if (this.isCacheValid(cached)) {
      this.logger.debug(
        `getAllTasks(${userId}) — cache hit (${cached!.data.length} tasks)`,
      );
      return cached!.data;
    }

    const courses = await this.getCourses(userId);
    const allTasks = (
      await Promise.all(
        courses.map((c) => this.getTasksByCourse(userId, c.id, c.name)),
      )
    )
      .flat()
      .sort((a, b) => {
        if (!a.dueDate && !b.dueDate) return 0;
        if (!a.dueDate) return 1;
        if (!b.dueDate) return -1;
        return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
      });

    this.allTasksCache.set(userId, { data: allTasks, timestamp: Date.now() });
    this.logger.log(
      `getAllTasks(${userId}) — fetched ${allTasks.length} tasks from Google (${courses.length} courses)`,
    );
    return allTasks;
  }

  /**
   * Invalida el cache de un usuario. Útil tras un re-link de Classroom
   * o un sync manual forzado.
   */
  invalidateCache(userId: number): void {
    this.coursesCache.delete(userId);
    this.allTasksCache.delete(userId);
    this.logger.debug(`Cache invalidated for userId=${userId}`);
  }
}
