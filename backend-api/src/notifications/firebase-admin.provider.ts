import { Logger } from '@nestjs/common';
import * as admin from 'firebase-admin';
import * as fs from 'fs';
import * as path from 'path';

export const FIREBASE_ADMIN = 'FIREBASE_ADMIN';

/**
 * Provider que inicializa (una sola vez) el Firebase Admin SDK.
 *
 * Busca la service-account key en este orden:
 *  1. `FIREBASE_SERVICE_ACCOUNT_PATH` env var → ruta a un JSON en disco
 *  2. `FIREBASE_SERVICE_ACCOUNT_JSON` env var → contenido del JSON inline
 *  3. Archivo `fcm-service-account.json` en la raíz del backend
 *
 * Si no encuentra credenciales devuelve `null` y el servicio loguea una
 * advertencia: el backend arranca igual pero los pushes son no-op.
 */
export const firebaseAdminProvider = {
  provide: FIREBASE_ADMIN,
  useFactory: (): admin.app.App | null => {
    const logger = new Logger('FirebaseAdmin');

    if (admin.apps.length > 0) {
      return admin.apps[0] ?? null;
    }

    try {
      const credential = resolveCredential();
      if (!credential) {
        logger.warn(
          'No se encontró service account de Firebase. Los pushes estarán deshabilitados. ' +
            'Configura FIREBASE_SERVICE_ACCOUNT_PATH o coloca fcm-service-account.json en la raíz.',
        );
        return null;
      }

      const app = admin.initializeApp({ credential });
      logger.log(
        `Firebase Admin inicializado (project: ${app.options.projectId})`,
      );
      return app;
    } catch (err) {
      logger.error('Error al inicializar Firebase Admin', err as Error);
      return null;
    }
  },
};

function resolveCredential(): admin.credential.Credential | null {
  const explicitPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (explicitPath && fs.existsSync(explicitPath)) {
    return admin.credential.cert(explicitPath);
  }

  const inlineJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (inlineJson) {
    return admin.credential.cert(JSON.parse(inlineJson));
  }

  const defaultPath = path.resolve(process.cwd(), 'fcm-service-account.json');
  if (fs.existsSync(defaultPath)) {
    return admin.credential.cert(defaultPath);
  }

  return null;
}
