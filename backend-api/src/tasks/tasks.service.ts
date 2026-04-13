import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CreateTaskDto } from './dto/create-task.dto';
import { UpdateTaskDto } from './dto/update-task.dto';
import { Task } from './entities/task.entity';

function toMillis(value: Date | string | null | undefined): number | null {
  if (value == null) return null;
  if (value instanceof Date) return value.getTime();
  if (typeof value === 'string') {
    const parsed = new Date(value).getTime();
    return Number.isNaN(parsed) ? null : parsed;
  }
  return null;
}

@Injectable()
export class TasksService {
  constructor(
    @InjectRepository(Task)
    private readonly taskRepository: Repository<Task>,
  ) {}

  async create(createTaskDto: CreateTaskDto, userId: number): Promise<Task> {
    const task = this.taskRepository.create({
      ...createTaskDto,
      userId,
      reminderSent: false,
    });
    return await this.taskRepository.save(task);
  }

  async findAll(userId: number): Promise<Task[]> {
    return await this.taskRepository.find({
      where: { userId, isArchived: false },
      relations: ['subtasks'],
      order: { createdAt: 'DESC' },
    });
  }

  async findArchived(userId: number): Promise<Task[]> {
    return await this.taskRepository.find({
      where: { userId, isArchived: true },
      relations: ['subtasks'],
      order: { updatedAt: 'DESC' },
    });
  }

  async findOne(id: number, userId: number): Promise<Task> {
    const task = await this.taskRepository.findOne({
      where: { id, userId },
      relations: ['subtasks'],
    });
    if (!task) {
      throw new NotFoundException(`Task with ID ${id} not found`);
    }
    return task;
  }

  async update(
    id: number,
    updateTaskDto: UpdateTaskDto,
    userId: number,
  ): Promise<Task> {
    const task = await this.findOne(id, userId);

    Object.assign(task, updateTaskDto);

    if (task.dueDate != null) {
      task.reminderSent = false;
    }

    if (typeof task.dueDate === 'string') {
      task.dueDate = new Date(task.dueDate);
    }

    return await this.taskRepository.save(task);
  }

  async remove(id: number, userId: number): Promise<void> {
    const task = await this.findOne(id, userId);
    await this.taskRepository.remove(task);
  }
}
