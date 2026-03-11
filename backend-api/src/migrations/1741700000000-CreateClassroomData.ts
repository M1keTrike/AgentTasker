import { MigrationInterface, QueryRunner } from 'typeorm';

export class CreateClassroomData1741700000000 implements MigrationInterface {
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE "classroom_data" (
        "id" SERIAL PRIMARY KEY,
        "user_id" integer NOT NULL UNIQUE,
        "googleAccessToken" text,
        "googleRefreshToken" text,
        "googleTokenExpiresAt" TIMESTAMP,
        "createdAt" TIMESTAMP NOT NULL DEFAULT now(),
        "updatedAt" TIMESTAMP NOT NULL DEFAULT now(),
        CONSTRAINT "FK_classroom_data_user" FOREIGN KEY ("user_id")
          REFERENCES "users"("id") ON DELETE CASCADE
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE "classroom_data"`);
  }
}
