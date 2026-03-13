import { Controller, Get, Param, Req, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../users/guards/jwt-auth.guard';
import { ClassroomService } from './classroom.service';
import { GoogleTokenService } from './services/google-token.service';

@Controller('classroom')
@UseGuards(JwtAuthGuard)
export class ClassroomController {
  constructor(
    private readonly classroomService: ClassroomService,
    private readonly googleTokenService: GoogleTokenService,
  ) {}

  @Get('status')
  async getStatus(@Req() req: { user: { id: number } }) {
    const connected = await this.googleTokenService.hasValidTokens(req.user.id);
    return { connected };
  }

  @Get('courses')
  getCourses(@Req() req: { user: { id: number } }) {
    return this.classroomService.getCourses(req.user.id);
  }

  @Get('courses/:courseId/tasks')
  getTasksByCourse(
    @Req() req: { user: { id: number } },
    @Param('courseId') courseId: string,
  ) {
    return this.classroomService.getTasksByCourse(req.user.id, courseId);
  }

  @Get('tasks')
  getAllTasks(@Req() req: { user: { id: number } }) {
    return this.classroomService.getAllTasks(req.user.id);
  }
}
