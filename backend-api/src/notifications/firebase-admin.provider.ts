import { Logger } from '@nestjs/common';
import * as admin from 'firebase-admin';
import * as fs from 'fs';
import * as path from 'path';

export const FIREBASE_ADMIN = 'FIREBASE_ADMIN';

interface ResolvedCredential {
  credential: admin.credential.Credential;
  projectId: string;
  source: string;
}

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
      const existing = admin.apps[0];
      if (existing) {
        logger.log(
          `Reusando Firebase Admin existente (project: ${existing.options.projectId ?? 'n/a'})`,
        );
      }
      return existing ?? null;
    }

    try {
      const resolved = resolveCredential();
      if (!resolved) {
        logger.warn(
          'No se encontró service account de Firebase. Los pushes estarán deshabilitados. ' +
            'Configura FIREBASE_SERVICE_ACCOUNT_PATH o coloca fcm-service-account.json en la raíz.',
        );
        return null;
      }

      // Pasamos `projectId` explícitamente para que `app.options.projectId`
      // quede poblado (si no, solo vive dentro de la credencial).
      const app = admin.initializeApp({
        credential: resolved.credential,
        projectId: resolved.projectId,
      });
      logger.log(
        `Firebase Admin inicializado (project: ${resolved.projectId}, source: ${resolved.source})`,
      );
      return app;
    } catch (err) {
      logger.error('Error al inicializar Firebase Admin', err as Error);
      return null;
    }
  },
};

/**
 * Lee el JSON del service account y devuelve la credential + projectId.
 * Intentamos las 3 fuentes en orden de prioridad.
 */
function resolveCredential(): ResolvedCredential | null {
  const logger = new Logger('FirebaseAdmin');

  // 1. Path explícito por env var
  const explicitPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (explicitPath && fs.existsSync(explicitPath)) {
    const json = JSON.parse(fs.readFileSync(explicitPath, 'utf8'));
    return {
      credential: admin.credential.cert(json),
      projectId: json.project_id,
      source: `env:FIREBASE_SERVICE_ACCOUNT_PATH (${explicitPath})`,
    };
  }

  // 2. JSON inline por env var
  const inlineJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (inlineJson) {
    try {
      const json = JSON.parse(inlineJson);
      return {
        credential: admin.credential.cert(json),
        projectId: json.project_id,
        source: 'env:FIREBASE_SERVICE_ACCOUNT_JSON',
      };
    } catch (e) {
      logger.warn(
        `FIREBASE_SERVICE_ACCOUNT_JSON no es JSON válido: ${(e as Error).message}`,
      );
    }
  }

  // 3. Archivo por defecto en la raíz del backend
  const defaultPath = path.resolve(process.cwd(), 'fcm-service-account.json');
  if (fs.existsSync(defaultPath)) {
    const json = JSON.parse(fs.readFileSync(defaultPath, 'utf8'));
    return {
      credential: admin.credential.cert(json),
      projectId: json.project_id,
      source: `file:${defaultPath}`,
    };
  }

  return null;
}
