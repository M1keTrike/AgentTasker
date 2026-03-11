import { Controller, Post, Body, Get, Query, Res, HttpCode, HttpStatus } from '@nestjs/common';
import type { Response } from 'express';
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
      dto.redirectUri,
    );
  }

  @Get('classroom/callback')
  classroomOAuthCallback(
    @Query('code') code: string,
    @Query('state') state: string,
    @Query('error') error: string,
    @Res() res: Response,
  ) {
    const params = new URLSearchParams();
    if (code) params.set('code', code);
    if (state) params.set('state', state);
    if (error) params.set('error', error);

    return res.redirect(`com.agentasker://oauth2redirect?${params.toString()}`);
  }
}
