import {
  Entity,
  Column,
  PrimaryGeneratedColumn,
  CreateDateColumn,
  UpdateDateColumn,
  OneToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

@Entity('classroom_data')
export class ClassroomData {
  @PrimaryGeneratedColumn()
  id: number;

  @OneToOne(() => User, (user) => user.classroomData, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ name: 'user_id', unique: true })
  userId: number;

  @Column({ type: 'text', nullable: true })
  googleAccessToken: string | null;

  @Column({ type: 'text', nullable: true })
  googleRefreshToken: string | null;

  @Column({ type: 'timestamp', nullable: true })
  googleTokenExpiresAt: Date | null;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
