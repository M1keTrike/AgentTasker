import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { KanbanColumn } from './entities/kanban-column.entity';
import { CreateKanbanColumnDto } from './dto/create-kanban-column.dto';
import { UpdateKanbanColumnDto } from './dto/update-kanban-column.dto';
import { ReorderItem } from './dto/reorder-kanban-columns.dto';

@Injectable()
export class KanbanService {
  constructor(
    @InjectRepository(KanbanColumn)
    private readonly columnRepository: Repository<KanbanColumn>,
  ) {}

  async findAll(userId: number): Promise<KanbanColumn[]> {
    let columns = await this.columnRepository.find({
      where: { userId },
      order: { position: 'ASC' },
    });

    if (columns.length === 0) {
      columns = await this.seedDefaults(userId);
    }

    return columns;
  }

  async create(
    dto: CreateKanbanColumnDto,
    userId: number,
  ): Promise<KanbanColumn> {
    if (dto.position === undefined) {
      const maxPosition = await this.columnRepository
        .createQueryBuilder('col')
        .select('MAX(col.position)', 'max')
        .where('col.userId = :userId', { userId })
        .getRawOne();
      dto.position = (maxPosition?.max ?? -1) + 1;
    }

    const column = this.columnRepository.create({ ...dto, userId });
    return await this.columnRepository.save(column);
  }

  async update(
    id: number,
    dto: UpdateKanbanColumnDto,
    userId: number,
  ): Promise<KanbanColumn> {
    const column = await this.findOne(id, userId);
    Object.assign(column, dto);
    return await this.columnRepository.save(column);
  }

  async remove(id: number, userId: number): Promise<void> {
    const column = await this.findOne(id, userId);
    await this.columnRepository.remove(column);
  }

  async reorder(items: ReorderItem[], userId: number): Promise<KanbanColumn[]> {
    for (const item of items) {
      await this.columnRepository.update(
        { id: item.id, userId },
        { position: item.position },
      );
    }
    return this.findAll(userId);
  }

  private async findOne(id: number, userId: number): Promise<KanbanColumn> {
    const column = await this.columnRepository.findOne({
      where: { id, userId },
    });
    if (!column) {
      throw new NotFoundException(`Kanban column with ID ${id} not found`);
    }
    return column;
  }

  private async seedDefaults(userId: number): Promise<KanbanColumn[]> {
    const defaults = [
      { title: 'Pendiente', statusKey: 'pending', position: 0, userId },
      { title: 'En Progreso', statusKey: 'in_progress', position: 1, userId },
      { title: 'Completado', statusKey: 'completed', position: 2, userId },
    ];

    const columns = this.columnRepository.create(defaults);
    return await this.columnRepository.save(columns);
  }
}
