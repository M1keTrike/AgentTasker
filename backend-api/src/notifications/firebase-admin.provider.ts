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

function resolveCredential(): ResolvedCredential | null {
  const logger = new Logger('FirebaseAdmin');

  const explicitPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (explicitPath && fs.existsSync(explicitPath)) {
    const json = JSON.parse(fs.readFileSync(explicitPath, 'utf8'));
    return {
      credential: admin.credential.cert(json),
      projectId: json.project_id,
      source: `env:FIREBASE_SERVICE_ACCOUNT_PATH (${explicitPath})`,
    };
  }

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
