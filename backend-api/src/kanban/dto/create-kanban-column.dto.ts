import { IsInt, IsNotEmpty, IsOptional, IsString } from 'class-validator';

export class CreateKanbanColumnDto {
  @IsString()
  @IsNotEmpty()
  title: string;

  @IsString()
  @IsNotEmpty()
  statusKey: string;

  @IsInt()
  @IsOptional()
  position?: number;

  @IsString()
  @IsOptional()
  color?: string;
}
