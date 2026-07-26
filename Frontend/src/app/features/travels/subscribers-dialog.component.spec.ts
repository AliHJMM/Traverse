import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SubscribersDialogComponent } from './subscribers-dialog.component';

describe('SubscribersDialogComponent', () => {
  let component: SubscribersDialogComponent;
  let fixture: ComponentFixture<SubscribersDialogComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [SubscribersDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MAT_DIALOG_DATA, useValue: { travelId: 1, travelTitle: 'Trip' } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscribersDialogComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('loads subscribers on init', () => {
    httpMock.expectOne('/api/travels/1/subscribers').flush([
      { travelerId: 2, subscribedAt: '2026-01-01T00:00:00Z' },
      { travelerId: 3, subscribedAt: '2026-01-02T00:00:00Z' },
    ]);
    expect(component.subscribers().length).toBe(2);
    expect(component.loading()).toBeFalse();
  });

  it('removes a subscriber', () => {
    httpMock
      .expectOne('/api/travels/1/subscribers')
      .flush([{ travelerId: 2, subscribedAt: '2026-01-01T00:00:00Z' }]);

    component.remove(2);
    httpMock.expectOne({ method: 'DELETE', url: '/api/travels/1/subscribers/2' }).flush(null);

    expect(component.subscribers().length).toBe(0);
  });
});
