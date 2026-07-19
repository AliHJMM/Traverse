export interface Destination {
  id?: number;
  city: string;
  country: string;
  arrivalDate: string | null;
  departureDate: string | null;
}

export interface Activity {
  id?: number;
  name: string;
  description: string | null;
  destinationCity: string | null;
  date: string | null;
  cost: number | null;
}

export interface Accommodation {
  id?: number;
  name: string;
  type: string;
  address: string | null;
  checkIn: string | null;
  checkOut: string | null;
}

export interface Transportation {
  id?: number;
  type: string;
  provider: string | null;
  fromLocation: string;
  toLocation: string;
  departureTime: string | null;
  arrivalTime: string | null;
}

export interface Travel {
  id: number;
  title: string;
  startDate: string;
  endDate: string;
  durationDays: number;
  destinations: Destination[];
  activities: Activity[];
  accommodations: Accommodation[];
  transportations: Transportation[];
  createdAt: string;
}

export interface TravelRequest {
  title: string;
  startDate: string;
  endDate: string;
  destinations: Destination[];
  activities: Activity[];
  accommodations: Accommodation[];
  transportations: Transportation[];
}

export interface NearbyDestination {
  city: string;
  country: string;
}
