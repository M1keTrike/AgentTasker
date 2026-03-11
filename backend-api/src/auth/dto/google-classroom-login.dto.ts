import { IsString, IsNotEmpty } from 'class-validator';

export class GoogleClassroomLoginDto {
  @IsString()
  @IsNotEmpty()
  idToken: string;

  @IsString()
  @IsNotEmpty()
  authorizationCode: string;
}
