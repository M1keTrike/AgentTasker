import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CreateTaskDto } from './dto/create-task.dto';
import { UpdateTaskDto } from './dto/update-task.dto';
import { Task } from './entities/task.entity';

/**
 * Convierte un Date, un string ISO o null/undefined a millis (epoch).
 * Robusto contra el hecho de que TypeORM devuelve `Date` al leer, pero
 * `class-validator` mantiene strings cuando el DTO entra con `@IsDateString`.
 */
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

    // `task.dueDate` viene de la DB como Date; el DTO la trae como string ISO.
    // Normalizamos a millis con un helper para poder compararlos sin crashes.
    const previousDueDateMillis = toMillis(task.dueDate);

    Object.assign(task, updateTaskDto);

    // Tras el assign, task.dueDate puede ser string (del DTO) o Date (si no
    // se envió). Re-normalizamos para comparar y, si cambió, convertimos el
    // string a Date real para que TypeORM lo persista como timestamp.
    const newDueDateMillis = toMillis(task.dueDate);

    if (previousDueDateMillis !== newDueDateMillis) {
      task.reminderSent = false;
    }

    // Asegurar tipo Date antes de save() — TypeORM puede aceptar el string,
    // pero así evitamos sorpresas en futuras reads.
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
