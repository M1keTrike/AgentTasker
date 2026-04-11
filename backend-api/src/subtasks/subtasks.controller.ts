import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseIntPipe,
  Patch,
  Post,
  Request,
  UseGuards,
} from '@nestjs/common';
import { SubtasksService } from './subtasks.service';
import { CreateSubtaskDto } from './dto/create-subtask.dto';
import { UpdateSubtaskDto } from './dto/update-subtask.dto';
import { BulkCreateSubtasksDto } from './dto/bulk-create-subtasks.dto';
import { JwtAuthGuard } from '../users/guards/jwt-auth.guard';

/**
 * Rutas:
 *   POST   /tasks/:taskId/subtasks         (single)
 *   POST   /tasks/:taskId/subtasks/bulk    (array, usado por el flujo IA)
 *   GET    /tasks/:taskId/subtasks
 *   PATCH  /subtasks/:id
 *   DELETE /subtasks/:id
 */
@UseGuards(JwtAuthGuard)
@Controller()
export class SubtasksController {
  constructor(private readonly subtasksService: SubtasksService) {}

  @Post('tasks/:taskId/subtasks')
  @HttpCode(HttpStatus.CREATED)
  create(
    @Param('taskId', ParseIntPipe) taskId: number,
    @Body() dto: CreateSubtaskDto,
    @Request() req,
  ) {
    return this.subtasksService.create(taskId, dto, req.user.id);
  }

  @Post('tasks/:taskId/subtasks/bulk')
  @HttpCode(HttpStatus.CREATED)
  createBulk(
    @Param('taskId', ParseIntPipe) taskId: number,
    @Body() dto: BulkCreateSubtasksDto,
    @Request() req,
  ) {
    return this.subtasksService.createBulk(taskId, dto, req.user.id);
  }

  @Get('tasks/:taskId/subtasks')
  findAll(
    @Param('taskId', ParseIntPipe) taskId: number,
    @Request() req,
  ) {
    return this.subtasksService.findAllByTask(taskId, req.user.id);
  }

  @Patch('subtasks/:id')
  update(
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateSubtaskDto,
    @Request() req,
  ) {
    return this.subtasksService.update(id, dto, req.user.id);
  }

  @Delete('subtasks/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(@Param('id', ParseIntPipe) id: number, @Request() req) {
    return this.subtasksService.remove(id, req.user.id);
  }
}
