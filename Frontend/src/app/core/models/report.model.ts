export type ReportSubjectType = 'MANAGER' | 'TRAVELER';
export type ReportStatus = 'OPEN' | 'REVIEWED';

export interface Report {
  id: number;
  reporterId: number;
  subjectType: ReportSubjectType;
  subjectId: number;
  travelId: number | null;
  reason: string;
  status: ReportStatus;
  createdAt: string;
}

export interface ReportRequest {
  subjectType: ReportSubjectType;
  subjectId: number;
  travelId: number | null;
  reason: string;
}
