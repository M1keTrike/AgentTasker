import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { KanbanService } from './kanban.service';
import { KanbanController } from './kanban.controller';
import { KanbanColumn } from './entities/kanban-column.entity';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [TypeOrmModule.forFeature([KanbanColumn]), UsersModule],
  controllers: [KanbanController],
  providers: [KanbanService],
  exports: [KanbanService],
})
export class KanbanModule {}
