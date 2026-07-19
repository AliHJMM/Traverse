export type Role = 'ADMIN' | 'USER';

export interface CurrentUser {
  id: number;
  email: string;
  role: Role;
}
