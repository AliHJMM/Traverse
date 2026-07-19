import { Role } from './current-user.model';

export interface User {
  id: number;
  email: string;
  role: Role;
  fullName: string;
  phone: string | null;
  address: string | null;
  enabled: boolean;
  createdAt: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  role: Role;
  fullName: string;
  phone?: string;
  address?: string;
}

export interface UpdateUserRequest {
  email?: string;
  role?: Role;
  enabled?: boolean;
  fullName?: string;
  phone?: string;
  address?: string;
}
