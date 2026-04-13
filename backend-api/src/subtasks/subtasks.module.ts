import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SubtasksService } from './subtasks.service';
import { SubtasksController } from './subtasks.controller';
import { Subtask } from './entities/subtask.entity';
import { Task } from '../tasks/entities/task.entity';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [TypeOrmModule.forFeature([Subtask, Task]), UsersModule],
  controllers: [SubtasksController],
  providers: [SubtasksService],
  exports: [SubtasksService],
})
export class SubtasksModule {}
