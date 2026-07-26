import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { TravelSearchResult } from '../../core/models/search.model';
import { Travel } from '../../core/models/travel.model';
import { SearchService } from '../../core/services/search.service';
import { SubscriptionService } from '../../core/services/subscription.service';
import { TravelService } from '../../core/services/travel.service';

@Component({
  selector: 'app-browse',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    RouterLink,
  ],
  templateUrl: './browse.component.html',
})
export class BrowseComponent {
  private readonly searchService = inject(SearchService);
  private readonly travelService = inject(TravelService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly snackBar = inject(MatSnackBar);

  readonly searchControl = new FormControl('', { nonNullable: true });

  readonly suggestions = signal<string[]>([]);
  readonly results = signal<TravelSearchResult[]>([]);
  readonly recommendations = signal<Travel[]>([]);
  readonly allTravels = signal<Travel[]>([]);
  readonly subscribedIds = signal<Set<number>>(new Set());

  readonly searching = signal(false);
  readonly loading = signal(true);
  /** True once the user has typed a query (drives search-results vs browse view). */
  readonly hasQuery = signal(false);

  constructor() {
    this.loadBrowseData();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((value) => this.onQueryChanged(value));
  }

  private loadBrowseData(): void {
    this.travelService.recommendations().subscribe({
      next: (travels) => this.recommendations.set(travels),
      error: () => this.recommendations.set([]),
    });
    this.travelService.findAll().subscribe({
      next: (travels) => {
        this.allTravels.set(travels);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.subscriptionService.mine().subscribe({
      next: (subs) =>
        this.subscribedIds.set(
          new Set(subs.filter((s) => s.status === 'SUBSCRIBED').map((s) => s.travelId)),
        ),
      error: () => this.subscribedIds.set(new Set()),
    });
  }

  private onQueryChanged(value: string): void {
    const query = value.trim();
    if (query.length === 0) {
      this.hasQuery.set(false);
      this.results.set([]);
      this.suggestions.set([]);
      return;
    }

    this.hasQuery.set(true);
    this.searching.set(true);

    this.searchService.autocomplete(query).subscribe({
      next: (res) => this.suggestions.set(res.suggestions),
      error: () => this.suggestions.set([]),
    });
    this.searchService.search(query).subscribe({
      next: (res) => {
        this.results.set(res);
        this.searching.set(false);
      },
      error: () => {
        this.results.set([]);
        this.searching.set(false);
      },
    });
  }

  isSubscribed(travelId: number): boolean {
    return this.subscribedIds().has(travelId);
  }

  subscribe(travelId: number): void {
    this.subscriptionService.subscribe(travelId).subscribe({
      next: () => {
        this.subscribedIds.update((set) => new Set(set).add(travelId));
        this.snackBar.open('Subscribed to trip.', 'Dismiss', { duration: 3000 });
      },
      error: (err) => {
        const message =
          err?.status === 409
            ? 'Subscriptions close 3 days before departure.'
            : 'Failed to subscribe.';
        this.snackBar.open(message, 'Dismiss', { duration: 4000 });
      },
    });
  }

  clearSearch(): void {
    this.searchControl.setValue('');
  }

  citiesOf(cities: string[] | null): string {
    return (cities ?? []).join(', ');
  }

  destinationSummary(travel: Travel): string {
    return travel.destinations.map((d) => d.city).join(', ');
  }
}
