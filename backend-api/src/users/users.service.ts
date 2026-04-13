import {
  Injectable,
  ConflictException,
  UnauthorizedException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { randomUUID } from 'crypto';
import { User } from './entities/user.entity';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    private readonly jwtService: JwtService,
  ) {}

  async register(registerDto: RegisterDto): Promise<{ message: string }> {
    const { username, email, password } = registerDto;

    const existingUser = await this.userRepository.findOne({
      where: [{ username }, { email }],
    });

    if (existingUser) {
      throw new ConflictException('Username or email already exists');
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const user = this.userRepository.create({
      username,
      email,
      password: hashedPassword,
    });

    await this.userRepository.save(user);

    return { message: 'User registered successfully' };
  }

  async login(
    loginDto: LoginDto,
  ): Promise<{
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    user: Partial<User>;
  }> {
    const { username, password } = loginDto;

    const user = await this.userRepository.findOne({
      where: { username },
    });

    if (!user || !user.password) {
      throw new UnauthorizedException('Invalid credentials');
    }

    const isPasswordValid = await bcrypt.compare(password, user.password);

    if (!isPasswordValid) {
      throw new UnauthorizedException('Invalid credentials');
    }

    const payload = { sub: user.id, username: user.username };
    const accessToken = await this.jwtService.signAsync(payload);
    const refreshToken = await this.generateRefreshToken(user.id);

    return {
      accessToken,
      refreshToken,
      expiresIn: 86400,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
      },
    };
  }

  async generateRefreshToken(userId: number): Promise<string> {
    const rawToken = randomUUID();
    const hashedToken = await bcrypt.hash(rawToken, 10);

    await this.userRepository.update(userId, { refreshToken: hashedToken });

    return rawToken;
  }

  async validateRefreshToken(
    userId: number,
    rawToken: string,
  ): Promise<boolean> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user || !user.refreshToken) return false;

    return bcrypt.compare(rawToken, user.refreshToken);
  }

  async invalidateRefreshToken(userId: number): Promise<void> {
    await this.userRepository.update(userId, { refreshToken: null });
  }

  async updateFcmToken(
    userId: number,
    fcmToken: string,
  ): Promise<{ message: string }> {
    await this.userRepository.update(userId, { fcmToken });
    return { message: 'FCM token updated successfully' };
  }

  async clearFcmToken(userId: number): Promise<void> {
    await this.userRepository.update(userId, { fcmToken: null });
  }

  async findById(id: number): Promise<User | null> {
    return this.userRepository.findOne({ where: { id } });
  }

  async findByUsername(username: string): Promise<User | null> {
    return this.userRepository.findOne({ where: { username } });
  }

  async findOrCreateFromGoogle(googleData: {
    googleId: string;
    email: string;
    displayName: string | null;
    photoUrl: string | null;
    emailVerified: boolean;
  }): Promise<User> {
    let user = await this.userRepository.findOne({
      where: { googleId: googleData.googleId },
    });

    if (user) {
      user.displayName = googleData.displayName;
      user.photoUrl = googleData.photoUrl;
      user.emailVerified = googleData.emailVerified;
      return this.userRepository.save(user);
    }

    user = await this.userRepository.findOne({
      where: { email: googleData.email },
    });

    if (user) {
      user.googleId = googleData.googleId;
      user.displayName = googleData.displayName;
      user.photoUrl = googleData.photoUrl;
      user.emailVerified = googleData.emailVerified;
      return this.userRepository.save(user);
    }

    const newUser = this.userRepository.create({
      email: googleData.email,
      googleId: googleData.googleId,
      displayName: googleData.displayName,
      photoUrl: googleData.photoUrl,
      emailVerified: googleData.emailVerified,
      username: null,
      password: null,
    });

    return this.userRepository.save(newUser);
  }
}
