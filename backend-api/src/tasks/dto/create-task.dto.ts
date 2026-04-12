import {
  IsDateString,
  IsEnum,
  IsNotEmpty,
  IsOptional,
  IsString,
} from 'class-validator';
import { TaskPriority, TaskSource, TaskStatus } from '../entities/task.entity';

export class CreateTaskDto {
  @IsString()
  @IsNotEmpty()
  title: string;

  @IsString()
  @IsOptional()
  description?: string;

  @IsEnum(TaskPriority)
  @IsOptional()
  priority?: TaskPriority;

  @IsEnum(TaskStatus)
  @IsOptional()
  status?: TaskStatus;

  @IsDateString()
  @IsOptional()
  dueDate?: string;

  @IsEnum(TaskSource)
  @IsOptional()
  source?: TaskSource;

  @IsString()
  @IsOptional()
  externalId?: string;

  @IsString()
  @IsOptional()
  courseName?: string;

  @IsString()
  @IsOptional()
  externalLink?: string;
}
