import {
  IsBoolean,
  IsInt,
  IsOptional,
  IsString,
  Min,
} from 'class-validator';

export class UpdateSubtaskDto {
  @IsString()
  @IsOptional()
  title?: string;

  @IsBoolean()
  @IsOptional()
  isCompleted?: boolean;

  @IsInt()
  @Min(0)
  @IsOptional()
  position?: number;
}
