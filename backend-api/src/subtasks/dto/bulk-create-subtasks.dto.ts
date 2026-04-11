import { ArrayMaxSize, ArrayMinSize, IsArray, IsString } from 'class-validator';

/**
 * Usado por el flujo IA (DeepSeek) que genera varias subtareas de golpe a
 * partir de la descripción de una task. Recibimos un array plano de strings
 * y el servicio se encarga de asignar `position` secuencial.
 */
export class BulkCreateSubtasksDto {
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(50)
  @IsString({ each: true })
  subtasks: string[];
}
