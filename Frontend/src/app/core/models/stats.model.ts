export interface ManagerStats {
  managerId: number;
  tripsCount: number;
  activeTravelersCount: number;
  totalIncome: number;
  averageRating: number;
  feedbackCount: number;
  reportCount: number;
}

export interface TravelerStats {
  travelerId: number;
  activeTrips: number;
  cancellations: number;
  feedbackGiven: number;
  reportsFiled: number;
}

export interface TravelStatSummary {
  travelId: number;
  title: string;
  managerId: number;
  subscribers: number;
  income: number;
  averageRating: number;
}

export interface MonthlyIncome {
  month: string;
  income: number;
}

export interface AdminOverview {
  totalManagers: number;
  totalTravels: number;
  totalActiveSubscriptions: number;
  totalIncome: number;
  openReports: number;
  topManagers: ManagerStats[];
  topTravels: TravelStatSummary[];
  monthlyIncome: MonthlyIncome[];
}
