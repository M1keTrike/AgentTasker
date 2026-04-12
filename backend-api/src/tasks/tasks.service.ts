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
    // Por defecto solo devolvemos las tasks NO archivadas. Las archivadas
    // se consultan explícitamente con findArchived() desde el Dashboard.
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

    // Siempre re-armamos el reminder cuando el usuario edita la task. Si
    // el dueDate ya venció y el cron ya disparó el push (reminderSent=true),
    // un edit desde la app significa que el usuario quiere que el reminder
    // pueda volver a funcionar — por ejemplo si actualiza la fecha o
    // simplemente quiere re-recibir el aviso tras cambiar el contenido.
    //
    // Antes solo lo reseteábamos cuando `dueDate` cambiaba, lo que dejaba
    // `reminderSent=true` en ediciones de título/descripción/prioridad.
    // Eso causaba que el cron nunca volviera a considerar la task.
    if (task.dueDate != null) {
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
