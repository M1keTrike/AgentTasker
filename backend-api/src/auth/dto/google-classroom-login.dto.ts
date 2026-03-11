import { IsString, IsNotEmpty, IsOptional } from 'class-validator';

export class GoogleClassroomLoginDto {
  @IsString()
  @IsNotEmpty()
  idToken: string;

  @IsString()
  @IsNotEmpty()
  authorizationCode: string;

  @IsString()
  @IsNotEmpty()
  redirectUri: string;

  @IsString()
  @IsOptional()
  codeVerifier?: string;
}
