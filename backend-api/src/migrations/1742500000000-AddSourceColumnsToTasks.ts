import { MigrationInterface, QueryRunner } from 'typeorm';

/**
 * Añade los campos necesarios para unificar tasks locales y de Classroom
 * en la misma tabla. `source` distingue el origen; `externalId` guarda el
 * courseWorkId de Classroom para idempotencia al re-sincronizar.
 */
export class AddSourceColumnsToTasks1742500000000
  implements MigrationInterface
{
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(
      `ALTER TABLE "tasks" ADD COLUMN "source" varchar NOT NULL DEFAULT 'local'`,
    );
    await queryRunner.query(
      `ALTER TABLE "tasks" ADD COLUMN "externalId" varchar NULL`,
    );
    await queryRunner.query(
      `ALTER TABLE "tasks" ADD COLUMN "courseName" varchar NULL`,
    );
    await queryRunner.query(
      `ALTER TABLE "tasks" ADD COLUMN "externalLink" varchar NULL`,
    );
    await queryRunner.query(
      `CREATE UNIQUE INDEX "idx_tasks_user_external"
         ON "tasks" ("userId", "externalId")
         WHERE "externalId" IS NOT NULL`,
    );
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP INDEX IF EXISTS "idx_tasks_user_external"`);
    await queryRunner.query(`ALTER TABLE "tasks" DROP COLUMN "externalLink"`);
    await queryRunner.query(`ALTER TABLE "tasks" DROP COLUMN "courseName"`);
    await queryRunner.query(`ALTER TABLE "tasks" DROP COLUMN "externalId"`);
    await queryRunner.query(`ALTER TABLE "tasks" DROP COLUMN "source"`);
  }
}
