import { MigrationInterface, QueryRunner } from 'typeorm';

export class AddUserIdToTasks1741800000000 implements MigrationInterface {
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(
      `ALTER TABLE "tasks" ADD COLUMN "userId" integer`,
    );
    await queryRunner.query(
      `ALTER TABLE "tasks" ADD CONSTRAINT "FK_tasks_user" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE`,
    );
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(
      `ALTER TABLE "tasks" DROP CONSTRAINT "FK_tasks_user"`,
    );
    await queryRunner.query(`ALTER TABLE "tasks" DROP COLUMN "userId"`);
  }
}
