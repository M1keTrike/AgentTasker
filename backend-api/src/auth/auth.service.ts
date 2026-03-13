import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { OAuth2Client } from 'google-auth-library';
import { UsersService } from '../users/users.service';
import { GoogleTokenService } from '../classroom/services/google-token.service';

@Injectable()
export class AuthService {
  private readonly googleClient: OAuth2Client;

  constructor(
    private readonly usersService: UsersService,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
    private readonly googleTokenService: GoogleTokenService,
  ) {
    this.googleClient = new OAuth2Client(
      this.configService.get<string>('GOOGLE_CLIENT_ID'),
    );
  }

  private async verifyGoogleIdToken(idToken: string) {
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

    return googlePayload;
  }

  private buildAuthResponse(
    user: { id: number; email: string; displayName: string | null; username: string | null; photoUrl: string | null; emailVerified: boolean },
    accessToken: string,
    idToken: string,
    refreshToken: string,
  ) {
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
        refreshToken,
        expiresIn: 86400,
      },
    };
  }

  async googleLogin(idToken: string) {
    const googlePayload = await this.verifyGoogleIdToken(idToken);

    const user = await this.usersService.findOrCreateFromGoogle({
      googleId: googlePayload.sub,
      email: googlePayload.email!,
      displayName: googlePayload.name ?? null,
      photoUrl: googlePayload.picture ?? null,
      emailVerified: googlePayload.email_verified ?? false,
    });

    const jwtPayload = { sub: user.id, username: user.username ?? user.email };
    const accessToken = await this.jwtService.signAsync(jwtPayload);
    const refreshToken = await this.usersService.generateRefreshToken(user.id);

    return this.buildAuthResponse(user, accessToken, idToken, refreshToken);
  }

  async googleClassroomLogin(idToken: string, authorizationCode: string, redirectUri: string, codeVerifier?: string) {
    const googlePayload = await this.verifyGoogleIdToken(idToken);

    const user = await this.usersService.findOrCreateFromGoogle({
      googleId: googlePayload.sub,
      email: googlePayload.email!,
      displayName: googlePayload.name ?? null,
      photoUrl: googlePayload.picture ?? null,
      emailVerified: googlePayload.email_verified ?? false,
    });

    const tokens = await this.googleTokenService.exchangeCode(authorizationCode, redirectUri, codeVerifier);
    await this.googleTokenService.saveTokens(user.id, tokens);

    const jwtPayload = { sub: user.id, username: user.username ?? user.email };
    const accessToken = await this.jwtService.signAsync(jwtPayload);
    const refreshToken = await this.usersService.generateRefreshToken(user.id);

    return this.buildAuthResponse(user, accessToken, idToken, refreshToken);
  }

  async refreshAccessToken(expiredAccessToken: string, oldRefreshToken: string) {
    let userId: number;

    try {
      const decoded = this.jwtService.decode(expiredAccessToken) as { sub: number; username: string } | null;
      if (!decoded || !decoded.sub) {
        throw new UnauthorizedException('Invalid access token');
      }
      userId = decoded.sub;
    } catch {
      throw new UnauthorizedException('Invalid access token');
    }

    const isValid = await this.usersService.validateRefreshToken(userId, oldRefreshToken);
    if (!isValid) {
      throw new UnauthorizedException('Invalid or expired refresh token');
    }

    const user = await this.usersService.findById(userId);
    if (!user) {
      throw new UnauthorizedException('User not found');
    }

    const jwtPayload = { sub: user.id, username: user.username ?? user.email };
    const accessToken = await this.jwtService.signAsync(jwtPayload);
    const newRefreshToken = await this.usersService.generateRefreshToken(user.id);

    return {
      accessToken,
      refreshToken: newRefreshToken,
      expiresIn: 86400,
    };
  }
}
