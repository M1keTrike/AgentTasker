import { MigrationInterface, QueryRunner } from 'typeorm';

export class CreateKanbanColumns1742100000000 implements MigrationInterface {
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE "kanban_columns" (
        "id" SERIAL PRIMARY KEY,
        "title" varchar NOT NULL,
        "statusKey" varchar NOT NULL,
        "position" integer NOT NULL DEFAULT 0,
        "color" varchar,
        "userId" integer NOT NULL,
        "createdAt" TIMESTAMP NOT NULL DEFAULT now(),
        "updatedAt" TIMESTAMP NOT NULL DEFAULT now(),
        CONSTRAINT "FK_kanban_columns_user" FOREIGN KEY ("userId")
          REFERENCES "users"("id") ON DELETE CASCADE
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE "kanban_columns"`);
  }
}
