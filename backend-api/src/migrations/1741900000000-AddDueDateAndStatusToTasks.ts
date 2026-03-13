import { MigrationInterface, QueryRunner } from 'typeorm';

export class AddDueDateAndStatusToTasks1741900000000
  implements MigrationInterface
{
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(
      `ALTER TABLE "tasks" ADD COLUMN "status" text NOT NULL DEFAULT 'pending'`,
    );
    await queryRunner.query(
      `ALTER TABLE "tasks" ADD COLUMN "dueDate" TIMESTAMP`,
    );
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`ALTER TABLE "tasks" DROP COLUMN "dueDate"`);
    await queryRunner.query(`ALTER TABLE "tasks" DROP COLUMN "status"`);
  }
}
