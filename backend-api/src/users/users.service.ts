import {
  Injectable,
  ConflictException,
  UnauthorizedException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
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

    // Verificar si el usuario ya existe
    const existingUser = await this.userRepository.findOne({
      where: [{ username }, { email }],
    });

    if (existingUser) {
      throw new ConflictException('Username or email already exists');
    }

    // Hashear la contraseña
    const hashedPassword = await bcrypt.hash(password, 10);

    // Crear el usuario
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
  ): Promise<{ accessToken: string; user: Partial<User> }> {
    const { username, password } = loginDto;

    // Buscar usuario
    const user = await this.userRepository.findOne({
      where: { username },
    });

    if (!user || !user.password) {
      throw new UnauthorizedException('Invalid credentials');
    }

    // Verificar contraseña
    const isPasswordValid = await bcrypt.compare(password, user.password);

    if (!isPasswordValid) {
      throw new UnauthorizedException('Invalid credentials');
    }

    // Generar JWT
    const payload = { sub: user.id, username: user.username };
    const accessToken = await this.jwtService.signAsync(payload);

    return {
      accessToken,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
      },
    };
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
    // 1. Try to find by googleId (returning user)
    let user = await this.userRepository.findOne({
      where: { googleId: googleData.googleId },
    });

    if (user) {
      user.displayName = googleData.displayName;
      user.photoUrl = googleData.photoUrl;
      user.emailVerified = googleData.emailVerified;
      return this.userRepository.save(user);
    }

    // 2. Try to find by email (existing internal account — migrate it)
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

    // 3. Create brand-new Google user
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
