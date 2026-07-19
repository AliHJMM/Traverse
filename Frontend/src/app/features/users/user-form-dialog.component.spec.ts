import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { User } from '../../core/models/user.model';
import { UserFormDialogComponent, UserFormDialogData } from './user-form-dialog.component';

describe('UserFormDialogComponent', () => {
  let component: UserFormDialogComponent;
  let fixture: ComponentFixture<UserFormDialogComponent>;
  let httpMock: HttpTestingController;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<UserFormDialogComponent>>;

  const existingUser: User = {
    id: 1,
    email: 'traveler@example.com',
    role: 'USER',
    fullName: 'Jane Traveler',
    phone: null,
    address: null,
    enabled: true,
    createdAt: '2026-01-01T00:00:00Z',
  };

  function setup(data: UserFormDialogData) {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    TestBed.configureTestingModule({
      imports: [UserFormDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormDialogComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => httpMock.verify());

  it('create mode requires a password and posts to /api/users', () => {
    setup({ mode: 'create' });

    component.form.setValue({
      email: 'new@example.com',
      password: 'password123',
      role: 'USER',
      fullName: 'New Traveler',
      phone: '',
      address: '',
      enabled: true,
    });
    component.submit();

    const req = httpMock.expectOne({ method: 'POST', url: '/api/users' });
    expect(req.request.body.password).toBe('password123');
    req.flush(existingUser);

    expect(dialogRefSpy.close).toHaveBeenCalledWith(existingUser);
  });

  it('create mode rejects a short password', () => {
    setup({ mode: 'create' });

    component.form.setValue({
      email: 'new@example.com',
      password: 'short',
      role: 'USER',
      fullName: 'New Traveler',
      phone: '',
      address: '',
      enabled: true,
    });
    component.submit();

    expect(component.form.invalid).toBeTrue();
    httpMock.expectNone('/api/users');
  });

  it('edit mode does not require a password and PUTs to /api/users/:id', () => {
    setup({ mode: 'edit', user: existingUser });

    expect(component.form.controls.email.value).toBe(existingUser.email);
    component.form.patchValue({ fullName: 'Jane T. Updated' });
    component.submit();

    const req = httpMock.expectOne({ method: 'PUT', url: '/api/users/1' });
    expect(req.request.body.fullName).toBe('Jane T. Updated');
    expect(req.request.body.password).toBeUndefined();
    req.flush({ ...existingUser, fullName: 'Jane T. Updated' });

    expect(dialogRefSpy.close).toHaveBeenCalled();
  });

  it('shows the server error message when the request fails', () => {
    setup({ mode: 'create' });

    component.form.setValue({
      email: 'dup@example.com',
      password: 'password123',
      role: 'USER',
      fullName: 'Dup',
      phone: '',
      address: '',
      enabled: true,
    });
    component.submit();

    httpMock
      .expectOne('/api/users')
      .flush({ error: 'Email already exists' }, { status: 409, statusText: 'Conflict' });

    expect(component.errorMessage()).toBe('Email already exists');
  });

  it('cancel closes without a result', () => {
    setup({ mode: 'create' });
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });
});
