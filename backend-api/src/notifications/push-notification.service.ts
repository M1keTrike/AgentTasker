import { Inject, Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as admin from 'firebase-admin';
import { User } from '../users/entities/user.entity';
import { FIREBASE_ADMIN } from './firebase-admin.provider';

export interface PushPayload {
  title: string;
  body: string;
  data?: Record<string, string>;
}

/**
 * Envía notificaciones push a un usuario específico vía FCM HTTP v1 API
 * (firebase-admin). Si el usuario no tiene `fcmToken` registrado, la llamada
 * se salta silenciosamente y loguea un warning.
 */
@Injectable()
export class PushNotificationService {
  private readonly logger = new Logger(PushNotificationService.name);

  constructor(
    @Inject(FIREBASE_ADMIN) private readonly firebaseApp: admin.app.App | null,
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
  ) {}

  async sendToUser(userId: number, payload: PushPayload): Promise<boolean> {
    if (!this.firebaseApp) {
      this.logger.warn(
        `Firebase Admin no inicializado. Push a userId=${userId} ignorado.`,
      );
      return false;
    }

    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user?.fcmToken) {
      this.logger.warn(
        `Usuario ${userId} sin fcmToken registrado. Push omitido.`,
      );
      return false;
    }

    return this.sendToToken(user.fcmToken, payload);
  }

  async sendToToken(token: string, payload: PushPayload): Promise<boolean> {
    if (!this.firebaseApp) return false;

    try {
      const messageId = await this.firebaseApp.messaging().send({
        token,
        notification: {
          title: payload.title,
          body: payload.body,
        },
        data: payload.data ?? {},
        android: {
          priority: 'high',
          notification: {
            channelId: 'push_notifications',
          },
        },
      });
      this.logger.log(`Push enviado: ${messageId}`);
      return true;
    } catch (err) {
      const error = err as admin.FirebaseError;
      this.logger.error(
        `Error enviando push: ${error.code ?? 'unknown'} - ${error.message}`,
      );
      // Si el token es inválido (unregistered / invalid-argument), limpiamos
      // el fcmToken del usuario para evitar reintentos.
      if (
        error.code === 'messaging/registration-token-not-registered' ||
        error.code === 'messaging/invalid-argument'
      ) {
        await this.userRepository
          .createQueryBuilder()
          .update(User)
          .set({ fcmToken: null })
          .where('fcmToken = :token', { token })
          .execute();
        this.logger.warn(
          'Token FCM inválido/no registrado. Limpiado del usuario.',
        );
      }
      return false;
    }
  }
}
