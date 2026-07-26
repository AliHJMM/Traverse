export interface TravelSearchResult {
  id: string;
  title: string;
  destinationCities: string[];
  destinationCountries: string[];
  activities: string[];
  accommodations: string[];
  transportationTypes: string[];
  startDate: string;
  endDate: string;
  durationDays: number;
  managerId: number | null;
}

export interface AutocompleteResponse {
  suggestions: string[];
}
