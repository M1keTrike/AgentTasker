import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { OAuth2Client } from 'google-auth-library';
import { UsersService } from '../users/users.service';

@Injectable()
export class AuthService {
  private readonly googleClient: OAuth2Client;

  constructor(
    private readonly usersService: UsersService,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {
    this.googleClient = new OAuth2Client(
      this.configService.get<string>('GOOGLE_CLIENT_ID'),
    );
  }

  async googleLogin(idToken: string) {
    let googlePayload;
    try {
      const ticket = await this.googleClient.verifyIdToken({
        idToken,
        audience: this.configService.get<string>('GOOGLE_CLIENT_ID'),
      });
      googlePayload = ticket.getPayload();
    } catch {
      throw new UnauthorizedException('Invalid or expired Google token');
    }

    if (!googlePayload || !googlePayload.email) {
      throw new UnauthorizedException('Invalid Google token payload');
    }

    const user = await this.usersService.findOrCreateFromGoogle({
      googleId: googlePayload.sub,
      email: googlePayload.email,
      displayName: googlePayload.name ?? null,
      photoUrl: googlePayload.picture ?? null,
      emailVerified: googlePayload.email_verified ?? false,
    });

    const jwtPayload = { sub: user.id, username: user.username ?? user.email };
    const accessToken = await this.jwtService.signAsync(jwtPayload);

    return {
      user: {
        id: user.id.toString(),
        email: user.email,
        displayName: user.displayName ?? user.username ?? '',
        photoUrl: user.photoUrl ?? '',
        emailVerified: user.emailVerified,
      },
      token: {
        accessToken,
        idToken,
        refreshToken: null,
        expiresIn: 86400,
      },
    };
  }
}
