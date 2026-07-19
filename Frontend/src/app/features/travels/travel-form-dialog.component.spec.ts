import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Travel } from '../../core/models/travel.model';
import { TravelFormDialogComponent, TravelFormDialogData } from './travel-form-dialog.component';

describe('TravelFormDialogComponent', () => {
  let component: TravelFormDialogComponent;
  let fixture: ComponentFixture<TravelFormDialogComponent>;
  let httpMock: HttpTestingController;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<TravelFormDialogComponent>>;

  const existingTravel: Travel = {
    id: 1,
    title: 'Europe Trip',
    startDate: '2026-08-01',
    endDate: '2026-08-10',
    durationDays: 10,
    destinations: [{ city: 'Paris', country: 'France', arrivalDate: null, departureDate: null }],
    activities: [],
    accommodations: [],
    transportations: [],
    createdAt: '2026-01-01T00:00:00Z',
  };

  function setup(data: TravelFormDialogData) {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    TestBed.configureTestingModule({
      imports: [TravelFormDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TravelFormDialogComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => httpMock.verify());

  it('create mode starts with no destinations and is invalid until one is added', () => {
    setup({ mode: 'create' });

    expect(component.destinations.length).toBe(0);
    expect(component.form.invalid).toBeTrue();

    component.addDestination();
    expect(component.destinations.length).toBe(1);
  });

  it('submits a new travel with a destination and posts to /api/travels', () => {
    setup({ mode: 'create' });

    component.form.patchValue({
      title: 'Europe Trip',
      startDate: new Date('2026-08-01'),
      endDate: new Date('2026-08-10'),
    });
    component.addDestination();
    component.destinations.at(0).patchValue({ city: 'Paris', country: 'France' });

    component.submit();

    const req = httpMock.expectOne({ method: 'POST', url: '/api/travels' });
    expect(req.request.body.title).toBe('Europe Trip');
    expect(req.request.body.destinations).toEqual([
      { city: 'Paris', country: 'France', arrivalDate: null, departureDate: null },
    ]);
    req.flush(existingTravel);

    expect(dialogRefSpy.close).toHaveBeenCalledWith(existingTravel);
  });

  it('edit mode pre-fills the form and PUTs to /api/travels/:id', () => {
    setup({ mode: 'edit', travel: existingTravel });

    expect(component.form.controls.title.value).toBe('Europe Trip');
    expect(component.destinations.length).toBe(1);

    component.submit();

    const req = httpMock.expectOne({ method: 'PUT', url: '/api/travels/1' });
    expect(req.request.body.title).toBe('Europe Trip');
    req.flush(existingTravel);

    expect(dialogRefSpy.close).toHaveBeenCalled();
  });

  it('removeDestination removes the destination at the given index', () => {
    setup({ mode: 'edit', travel: existingTravel });

    expect(component.destinations.length).toBe(1);
    component.removeDestination(0);
    expect(component.destinations.length).toBe(0);
  });

  it('shows the server error message when the request fails', () => {
    setup({ mode: 'create' });

    component.form.patchValue({
      title: 'Europe Trip',
      startDate: new Date('2026-08-01'),
      endDate: new Date('2026-08-10'),
    });
    component.addDestination();
    component.destinations.at(0).patchValue({ city: 'Paris', country: 'France' });

    component.submit();
    httpMock
      .expectOne('/api/travels')
      .flush({ error: 'title: must not be blank' }, { status: 400, statusText: 'Bad Request' });

    expect(component.errorMessage()).toBe('title: must not be blank');
  });

  it('cancel closes without a result', () => {
    setup({ mode: 'create' });
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });
});
