import {
  IsArray,
  IsInt,
  IsNotEmpty,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

export class ReorderItem {
  @IsInt()
  @IsNotEmpty()
  id: number;

  @IsInt()
  @IsNotEmpty()
  position: number;
}

export class ReorderKanbanColumnsDto {
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => ReorderItem)
  columns: ReorderItem[];
}
