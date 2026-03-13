import { Injectable, UnauthorizedException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import { OAuth2Client } from 'google-auth-library';
import { ClassroomData } from '../entities/classroom-data.entity';

@Injectable()
export class GoogleTokenService {
  private readonly oauth2Client: OAuth2Client;

  constructor(
    @InjectRepository(ClassroomData)
    private readonly classroomDataRepo: Repository<ClassroomData>,
    private readonly configService: ConfigService,
  ) {
    this.oauth2Client = new OAuth2Client(
      this.configService.get<string>('GOOGLE_CLIENT_ID'),
      this.configService.get<string>('GOOGLE_CLIENT_SECRET'),
    );
  }

  async exchangeCode(authorizationCode: string, redirectUri: string, codeVerifier?: string): Promise<{
    accessToken: string;
    refreshToken: string | null;
    expiryDate: number | null;
  }> {
    const { tokens } = await this.oauth2Client.getToken({
      code: authorizationCode,
      redirect_uri: redirectUri,
      ...(codeVerifier && { codeVerifier }),
    });
    return {
      accessToken: tokens.access_token ?? '',
      refreshToken: tokens.refresh_token ?? null,
      expiryDate: tokens.expiry_date ?? null,
    };
  }

  async saveTokens(
    userId: number,
    tokens: {
      accessToken: string;
      refreshToken: string | null;
      expiryDate: number | null;
    },
  ): Promise<void> {
    const existing = await this.classroomDataRepo.findOneBy({ userId });

    const tokenData = {
      googleAccessToken: tokens.accessToken,
      googleRefreshToken: tokens.refreshToken,
      googleTokenExpiresAt: tokens.expiryDate
        ? new Date(tokens.expiryDate)
        : null,
    };

    if (existing) {
      if (!tokens.refreshToken) {
        delete (tokenData as Record<string, unknown>).googleRefreshToken;
      }
      await this.classroomDataRepo.update(existing.id, tokenData);
    } else {
      await this.classroomDataRepo.save(
        this.classroomDataRepo.create({ userId, ...tokenData }),
      );
    }
  }

  async getValidAccessToken(userId: number): Promise<string> {
    const classroomData = await this.classroomDataRepo.findOneBy({ userId });

    if (!classroomData || !classroomData.googleAccessToken) {
      throw new UnauthorizedException(
        'No Google Classroom tokens found. Please connect Google Classroom first.',
      );
    }

    if (
      classroomData.googleTokenExpiresAt &&
      classroomData.googleTokenExpiresAt.getTime() > Date.now() + 5 * 60 * 1000
    ) {
      return classroomData.googleAccessToken;
    }

    if (!classroomData.googleRefreshToken) {
      throw new UnauthorizedException(
        'Google Classroom token expired and no refresh token available. Please re-authorize.',
      );
    }

    try {
      this.oauth2Client.setCredentials({
        refresh_token: classroomData.googleRefreshToken,
      });
      const { credentials } = await this.oauth2Client.refreshAccessToken();

      await this.classroomDataRepo.update(classroomData.id, {
        googleAccessToken: credentials.access_token,
        googleTokenExpiresAt: credentials.expiry_date
          ? new Date(credentials.expiry_date)
          : null,
      });

      return credentials.access_token!;
    } catch {
      throw new UnauthorizedException(
        'Failed to refresh Google token. Please re-authorize Google Classroom.',
      );
    }
  }

  async hasValidTokens(userId: number): Promise<boolean> {
    const classroomData = await this.classroomDataRepo.findOneBy({ userId });
    return !!(classroomData && classroomData.googleAccessToken);
  }
}
