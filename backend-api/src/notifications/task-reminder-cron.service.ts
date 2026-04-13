import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Between, IsNull, Not, Repository } from 'typeorm';
import { Task, TaskStatus } from '../tasks/entities/task.entity';
import { PushNotificationService } from './push-notification.service';

@Injectable()
export class TaskReminderCronService {
  private readonly logger = new Logger(TaskReminderCronService.name);

  constructor(
    @InjectRepository(Task)
    private readonly taskRepository: Repository<Task>,
    private readonly pushNotificationService: PushNotificationService,
  ) {}

  @Cron(CronExpression.EVERY_MINUTE)
  async dispatchDueReminders(): Promise<void> {
    const now = new Date();
    const windowStart = new Date(now.getTime() - 60 * 1000);
    const windowEnd = new Date(now.getTime() + 30 * 1000);

    const dueTasks = await this.taskRepository.find({
      where: {
        dueDate: Between(windowStart, windowEnd),
        reminderSent: false,
        userId: Not(IsNull()),
        status: Not(TaskStatus.COMPLETED),
      },
    });

    if (dueTasks.length === 0) return;

    this.logger.log(`Procesando ${dueTasks.length} recordatorio(s) pendiente(s)`);

    for (const task of dueTasks) {
      const sent = await this.pushNotificationService.sendToUser(task.userId, {
        title: 'Recordatorio de tarea',
        body: task.title,
        data: {
          screen: 'tasks',
          taskId: String(task.id),
          type: 'reminder',
        },
      });

      task.reminderSent = true;
      await this.taskRepository.save(task);

      this.logger.log(
        `Reminder task=${task.id} user=${task.userId} sent=${sent}`,
      );
    }
  }
}
