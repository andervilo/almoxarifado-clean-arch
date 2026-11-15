import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { authGuard, roleGuard } from './auth.guard';

describe('authGuard & roleGuard', () => {
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/estoque' } as RouterStateSnapshot;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'hasAnyRole']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authSpy }],
    });
    router = TestBed.inject(Router);
  });

  it('redireciona para /login quando nao autenticado', () => {
    authSpy.isAuthenticated.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Festoque');
  });

  it('permite acesso no roleGuard quando possui perfil', () => {
    authSpy.isAuthenticated.and.returnValue(true);
    authSpy.hasAnyRole.and.returnValue(true);
    const guard = roleGuard(['ALMOXARIFE']);
    const result = TestBed.runInInjectionContext(() => guard(route, state));
    expect(result).toBeTrue();
  });

  it('redireciona para /acesso-negado quando perfil nao autorizado', () => {
    authSpy.isAuthenticated.and.returnValue(true);
    authSpy.hasAnyRole.and.returnValue(false);
    const guard = roleGuard(['ADMIN_ORGANIZACAO']);
    const result = TestBed.runInInjectionContext(() => guard(route, state));
    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/acesso-negado');
  });
});
