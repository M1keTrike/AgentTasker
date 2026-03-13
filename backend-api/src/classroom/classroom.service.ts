import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { OAuth2Client } from 'google-auth-library';
import { GoogleTokenService } from './services/google-token.service';
import { ClassroomCourseDto } from './dto/classroom-course.dto';
import { ClassroomTaskDto } from './dto/classroom-task.dto';

const CLASSROOM_API = 'https://classroom.googleapis.com/v1';

@Injectable()
export class ClassroomService {
  private readonly oauth2Client: OAuth2Client;

  constructor(
    private readonly googleTokenService: GoogleTokenService,
    private readonly configService: ConfigService,
  ) {
    this.oauth2Client = new OAuth2Client(
      this.configService.get<string>('GOOGLE_CLIENT_ID'),
      this.configService.get<string>('GOOGLE_CLIENT_SECRET'),
    );
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

    return (
      data.courses?.map((c) => ({
        id: c.id,
        name: c.name,
        section: c.section,
        descriptionHeading: c.descriptionHeading,
        courseState: c.courseState,
        alternateLink: c.alternateLink,
      })) ?? []
    );
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
      } catch { }

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

  async getAllTasks(userId: number): Promise<ClassroomTaskDto[]> {
    const courses = await this.getCourses(userId);
    const allTasks = await Promise.all(
      courses.map((c) => this.getTasksByCourse(userId, c.id, c.name)),
    );
    return allTasks.flat().sort((a, b) => {
      if (!a.dueDate && !b.dueDate) return 0;
      if (!a.dueDate) return 1;
      if (!b.dueDate) return -1;
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    });
  }
}
