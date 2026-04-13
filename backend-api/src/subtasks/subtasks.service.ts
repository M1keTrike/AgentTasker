import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Subtask } from './entities/subtask.entity';
import { Task } from '../tasks/entities/task.entity';
import { CreateSubtaskDto } from './dto/create-subtask.dto';
import { UpdateSubtaskDto } from './dto/update-subtask.dto';
import { BulkCreateSubtasksDto } from './dto/bulk-create-subtasks.dto';

@Injectable()
export class SubtasksService {
  constructor(
    @InjectRepository(Subtask)
    private readonly subtaskRepository: Repository<Subtask>,
    @InjectRepository(Task)
    private readonly taskRepository: Repository<Task>,
  ) {}

  private async assertTaskOwnership(
    taskId: number,
    userId: number,
  ): Promise<Task> {
    const task = await this.taskRepository.findOne({
      where: { id: taskId, userId },
    });
    if (!task) {
      throw new NotFoundException(`Task with ID ${taskId} not found`);
    }
    return task;
  }

  async create(
    taskId: number,
    dto: CreateSubtaskDto,
    userId: number,
  ): Promise<Subtask> {
    await this.assertTaskOwnership(taskId, userId);

    let position = dto.position;
    if (position === undefined) {
      const count = await this.subtaskRepository.count({ where: { taskId } });
      position = count;
    }

    const subtask = this.subtaskRepository.create({
      title: dto.title,
      isCompleted: dto.isCompleted ?? false,
      position,
      taskId,
    });
    return await this.subtaskRepository.save(subtask);
  }

  async createBulk(
    taskId: number,
    dto: BulkCreateSubtasksDto,
    userId: number,
  ): Promise<Subtask[]> {
    await this.assertTaskOwnership(taskId, userId);

    const existingCount = await this.subtaskRepository.count({
      where: { taskId },
    });

    const entities = dto.subtasks.map((title, idx) =>
      this.subtaskRepository.create({
        title,
        isCompleted: false,
        position: existingCount + idx,
        taskId,
      }),
    );

    return await this.subtaskRepository.save(entities);
  }

  async findAllByTask(taskId: number, userId: number): Promise<Subtask[]> {
    await this.assertTaskOwnership(taskId, userId);
    return await this.subtaskRepository.find({
      where: { taskId },
      order: { position: 'ASC' },
    });
  }

  async update(
    id: number,
    dto: UpdateSubtaskDto,
    userId: number,
  ): Promise<Subtask> {
    const subtask = await this.subtaskRepository.findOne({ where: { id } });
    if (!subtask) {
      throw new NotFoundException(`Subtask with ID ${id} not found`);
    }
    await this.assertTaskOwnership(subtask.taskId, userId);

    Object.assign(subtask, dto);
    return await this.subtaskRepository.save(subtask);
  }

  async remove(id: number, userId: number): Promise<void> {
    const subtask = await this.subtaskRepository.findOne({ where: { id } });
    if (!subtask) {
      throw new NotFoundException(`Subtask with ID ${id} not found`);
    }
    await this.assertTaskOwnership(subtask.taskId, userId);
    await this.subtaskRepository.remove(subtask);
  }
}
