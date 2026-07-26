export interface Feedback {
  id: number;
  travelId: number;
  travelerId: number;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface FeedbackRequest {
  rating: number;
  comment: string | null;
}
