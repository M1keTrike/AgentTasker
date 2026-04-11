import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { User } from '../users/entities/user.entity';
import { Task } from '../tasks/entities/task.entity';
import { firebaseAdminProvider } from './firebase-admin.provider';
import { PushNotificationService } from './push-notification.service';
import { TaskReminderCronService } from './task-reminder-cron.service';

@Module({
  imports: [TypeOrmModule.forFeature([User, Task])],
  providers: [
    firebaseAdminProvider,
    PushNotificationService,
    TaskReminderCronService,
  ],
  exports: [PushNotificationService],
})
export class NotificationsModule {}
