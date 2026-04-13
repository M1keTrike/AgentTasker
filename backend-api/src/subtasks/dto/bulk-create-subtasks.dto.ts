import { ArrayMaxSize, ArrayMinSize, IsArray, IsString } from 'class-validator';

export class BulkCreateSubtasksDto {
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(50)
  @IsString({ each: true })
  subtasks: string[];
}
