import { PartialType } from '@nestjs/mapped-types';
import { IsBoolean, IsOptional } from 'class-validator';
import { CreateTaskDto } from './create-task.dto';

export class UpdateTaskDto extends PartialType(CreateTaskDto) {
  /**
   * Se setea a `true` desde el botón "Completar" del TaskCard, que además
   * marca la task como completed. La task deja de aparecer en el listado
   * principal y va a "Archivados" en el Dashboard.
   */
  @IsBoolean()
  @IsOptional()
  isArchived?: boolean;
}
