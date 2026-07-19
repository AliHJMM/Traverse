import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { User } from '../../core/models/user.model';
import { UsersListComponent } from './users-list.component';

describe('UsersListComponent', () => {
  let component: UsersListComponent;
  let fixture: ComponentFixture<UsersListComponent>;
  let httpMock: HttpTestingController;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const user: User = {
    id: 1,
    email: 'traveler@example.com',
    role: 'USER',
    fullName: 'Jane Traveler',
    phone: null,
    address: null,
    enabled: true,
    createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [UsersListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UsersListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpMock.expectOne('/api/users').flush([user]);
  });

  afterEach(() => httpMock.verify());

  it('loads users on init', () => {
    expect(component.users()).toEqual([user]);
    expect(component.loading()).toBeFalse();
  });

  it('reloads the list after a successful create dialog', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(user) } as ReturnType<MatDialog['open']>);

    component.openCreateDialog();
    expect(dialogSpy.open).toHaveBeenCalled();

    httpMock.expectOne('/api/users').flush([user, { ...user, id: 2 }]);
    expect(component.users().length).toBe(2);
  });

  it('deletes a user after confirmation', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as ReturnType<MatDialog['open']>);

    component.confirmDelete(user);
    expect(dialogSpy.open).toHaveBeenCalled();

    httpMock.expectOne({ method: 'DELETE', url: '/api/users/1' }).flush(null);
    httpMock.expectOne('/api/users').flush([]);

    expect(component.users()).toEqual([]);
  });

  it('does not delete when the confirmation is cancelled', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as ReturnType<MatDialog['open']>);

    component.confirmDelete(user);
    httpMock.expectNone({ method: 'DELETE', url: '/api/users/1' });
  });
});
