import {
  Entity,
  Column,
  PrimaryGeneratedColumn,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Subtask } from '../../subtasks/entities/subtask.entity';

export enum TaskPriority {
  HIGH = 'high',
  MEDIUM = 'medium',
  LOW = 'low',
}

export enum TaskStatus {
  PENDING = 'pending',
  IN_PROGRESS = 'in_progress',
  COMPLETED = 'completed',
}

/**
 * Origen de la task. "local" es el default (tasks creadas en la app),
 * "classroom" las importadas desde Google Classroom vía el sync del
 * Dashboard. Se mapea al mismo TaskEntity para unificar el modelo.
 */
export enum TaskSource {
  LOCAL = 'local',
  CLASSROOM = 'classroom',
}

@Entity('tasks')
export class Task {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  title: string;

  @Column('text')
  description: string;

  @Column({
    type: 'text',
    enum: TaskPriority,
    default: TaskPriority.MEDIUM,
  })
  priority: TaskPriority;

  @Column({
    type: 'text',
    enum: TaskStatus,
    default: TaskStatus.PENDING,
  })
  status: TaskStatus;

  @Column({ type: 'timestamp', nullable: true })
  dueDate: Date | null;

  @Column({ type: 'boolean', default: false })
  reminderSent: boolean;

  @Column({
    type: 'text',
    enum: TaskSource,
    default: TaskSource.LOCAL,
  })
  source: TaskSource;

  @Column({ type: 'varchar', nullable: true })
  externalId: string | null;

  @Column({ type: 'varchar', nullable: true })
  courseName: string | null;

  @Column({ type: 'varchar', nullable: true })
  externalLink: string | null;

  @Column({ nullable: true })
  userId: number;

  @ManyToOne(() => User, (user) => user.tasks, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'userId' })
  user: User;

  @OneToMany(() => Subtask, (subtask) => subtask.task, { cascade: true })
  subtasks: Subtask[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
