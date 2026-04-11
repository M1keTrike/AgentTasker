import { MigrationInterface, QueryRunner } from 'typeorm';

export class AddSubtasksTable1742400000000 implements MigrationInterface {
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE "subtasks" (
        "id" SERIAL PRIMARY KEY,
        "title" varchar NOT NULL,
        "isCompleted" boolean NOT NULL DEFAULT false,
        "position" integer NOT NULL DEFAULT 0,
        "taskId" integer NOT NULL,
        "createdAt" TIMESTAMP NOT NULL DEFAULT now(),
        "updatedAt" TIMESTAMP NOT NULL DEFAULT now(),
        CONSTRAINT "fk_subtasks_task" FOREIGN KEY ("taskId")
          REFERENCES "tasks"("id") ON DELETE CASCADE
      )
    `);
    await queryRunner.query(
      `CREATE INDEX "idx_subtasks_taskId" ON "subtasks" ("taskId")`,
    );
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP INDEX IF EXISTS "idx_subtasks_taskId"`);
    await queryRunner.query(`DROP TABLE IF EXISTS "subtasks"`);
  }
}
