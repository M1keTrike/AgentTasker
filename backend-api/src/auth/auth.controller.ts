import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { AuthService } from './auth.service';
import { GoogleLoginDto } from './dto/google-login.dto';
import { GoogleClassroomLoginDto } from './dto/google-classroom-login.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('google')
  @HttpCode(HttpStatus.OK)
  googleLogin(@Body() dto: GoogleLoginDto) {
    return this.authService.googleLogin(dto.idToken);
  }

  @Post('google-classroom')
  @HttpCode(HttpStatus.OK)
  googleClassroomLogin(@Body() dto: GoogleClassroomLoginDto) {
    return this.authService.googleClassroomLogin(
      dto.idToken,
      dto.authorizationCode,
    );
  }
}
