import {
  Controller,
  Get,
  Post,
  Body,
  Patch,
  Param,
  Delete,
  HttpCode,
  HttpStatus,
  UseGuards,
  Request,
} from '@nestjs/common';
import { KanbanService } from './kanban.service';
import { CreateKanbanColumnDto } from './dto/create-kanban-column.dto';
import { UpdateKanbanColumnDto } from './dto/update-kanban-column.dto';
import { ReorderKanbanColumnsDto } from './dto/reorder-kanban-columns.dto';
import { JwtAuthGuard } from '../users/guards/jwt-auth.guard';

@Controller('kanban')
@UseGuards(JwtAuthGuard)
export class KanbanController {
  constructor(private readonly kanbanService: KanbanService) {}

  @Get('columns')
  findAll(@Request() req) {
    return this.kanbanService.findAll(req.user.id);
  }

  @Post('columns')
  @HttpCode(HttpStatus.CREATED)
  create(@Body() dto: CreateKanbanColumnDto, @Request() req) {
    return this.kanbanService.create(dto, req.user.id);
  }

  @Patch('columns/reorder')
  reorder(@Body() dto: ReorderKanbanColumnsDto, @Request() req) {
    return this.kanbanService.reorder(dto.columns, req.user.id);
  }

  @Patch('columns/:id')
  update(
    @Param('id') id: string,
    @Body() dto: UpdateKanbanColumnDto,
    @Request() req,
  ) {
    return this.kanbanService.update(+id, dto, req.user.id);
  }

  @Delete('columns/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(@Param('id') id: string, @Request() req) {
    return this.kanbanService.remove(+id, req.user.id);
  }
}
