export type Role = 'ADMIN' | 'TRAVEL_MANAGER' | 'TRAVELER';

export interface CurrentUser {
  id: number;
  email: string;
  role: Role;
}
