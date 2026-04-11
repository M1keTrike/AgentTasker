import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CreateTaskDto } from './dto/create-task.dto';
import { UpdateTaskDto } from './dto/update-task.dto';
import { Task } from './entities/task.entity';

@Injectable()
export class TasksService {
  constructor(
    @InjectRepository(Task)
    private readonly taskRepository: Repository<Task>,
  ) {}

  async create(createTaskDto: CreateTaskDto, userId: number): Promise<Task> {
    // Una tarea recién creada siempre arranca con reminderSent=false para
    // que el cron pueda dispararla cuando llegue su dueDate.
    const task = this.taskRepository.create({
      ...createTaskDto,
      userId,
      reminderSent: false,
    });
    return await this.taskRepository.save(task);
  }

  async findAll(userId: number): Promise<Task[]> {
    return await this.taskRepository.find({ where: { userId } });
  }

  async findOne(id: number, userId: number): Promise<Task> {
    const task = await this.taskRepository.findOne({
      where: { id, userId },
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
    const previousDueDate = task.dueDate?.getTime() ?? null;
    Object.assign(task, updateTaskDto);

    // Si el usuario cambió el dueDate, reseteamos reminderSent para que
    // el cron lo vuelva a disparar con la nueva fecha.
    const newDueDate = task.dueDate?.getTime() ?? null;
    if (previousDueDate !== newDueDate) {
      task.reminderSent = false;
    }

    return await this.taskRepository.save(task);
  }

  async remove(id: number, userId: number): Promise<void> {
    const task = await this.findOne(id, userId);
    await this.taskRepository.remove(task);
  }
}
