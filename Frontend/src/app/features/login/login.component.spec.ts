import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('marks the form invalid and does not submit when fields are empty', () => {
    component.submit();
    expect(component.form.invalid).toBeTrue();
    httpMock.expectNone('/api/auth/login');
  });

  it('navigates to /users on successful login', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');
    component.form.setValue({ email: 'admin@example.com', password: 'password123' });

    component.submit();
    httpMock.expectOne('/api/auth/login').flush({ id: 1, email: 'admin@example.com', role: 'ADMIN' });

    expect(navigateSpy).toHaveBeenCalledWith('/users');
    expect(component.loading()).toBeFalse();
  });

  it('shows an error message on failed login', () => {
    component.form.setValue({ email: 'admin@example.com', password: 'wrong' });

    component.submit();
    httpMock.expectOne('/api/auth/login').flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(component.errorMessage()).toBe('Invalid email or password.');
    expect(component.loading()).toBeFalse();
  });
});
