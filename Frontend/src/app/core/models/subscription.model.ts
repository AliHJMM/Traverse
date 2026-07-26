export type SubscriptionStatus = 'SUBSCRIBED' | 'CANCELLED';

export interface Subscription {
  id: number;
  travelId: number;
  travelerId: number;
  status: SubscriptionStatus;
  createdAt: string;
}

export interface Subscriber {
  travelerId: number;
  subscribedAt: string;
}
