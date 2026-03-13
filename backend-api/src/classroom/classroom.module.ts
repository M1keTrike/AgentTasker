import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ClassroomData } from './entities/classroom-data.entity';
import { ClassroomController } from './classroom.controller';
import { ClassroomService } from './classroom.service';
import { GoogleTokenService } from './services/google-token.service';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [TypeOrmModule.forFeature([ClassroomData]), UsersModule],
  controllers: [ClassroomController],
  providers: [ClassroomService, GoogleTokenService],
  exports: [ClassroomService],
})
export class ClassroomModule {}
