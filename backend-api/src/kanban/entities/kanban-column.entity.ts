import {
  Entity,
  Column,
  PrimaryGeneratedColumn,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

@Entity('kanban_columns')
export class KanbanColumn {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  title: string;

  @Column()
  statusKey: string;

  @Column({ default: 0 })
  position: number;

  @Column({ type: 'varchar', nullable: true })
  color: string | null;

  @Column()
  userId: number;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'userId' })
  user: User;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
