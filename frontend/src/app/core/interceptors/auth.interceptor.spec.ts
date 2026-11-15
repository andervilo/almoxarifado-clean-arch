import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { APP_ENVIRONMENT } from '../config/app-environment';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['accessToken', 'clearSessionLocal']);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authSpy },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adiciona Authorization Bearer em chamadas para a API', () => {
    authSpy.accessToken.and.returnValue('jwt-token-123');
    http.get(`${APP_ENVIRONMENT.apiBaseUrl}/auth/me`).subscribe();

    const req = httpMock.expectOne(`${APP_ENVIRONMENT.apiBaseUrl}/auth/me`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token-123');
    req.flush({});
  });

  it('limpa sessao e redireciona para /login em 401', () => {
    authSpy.accessToken.and.returnValue('jwt-expirado');
    spyOn(router, 'navigate').and.resolveTo(true);

    http.get(`${APP_ENVIRONMENT.apiBaseUrl}/dashboard`).subscribe({
      error: () => {
        expect(authSpy.clearSessionLocal).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
      },
    });

    const req = httpMock.expectOne(`${APP_ENVIRONMENT.apiBaseUrl}/dashboard`);
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
  });
});
