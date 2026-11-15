import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { APP_ENVIRONMENT } from '../config/app-environment';
import { AuthService } from './auth.service';

function createJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.sig`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('rejeita callback com state divergente', async () => {
    sessionStorage.setItem('almox_pkce_state', 'estado-esperado');
    sessionStorage.setItem('almox_pkce_verifier', 'verifier-123');

    const ok = await service.handleCallback('code-abc', 'estado-errado');
    expect(ok).toBeFalse();
    expect(service.error()).toContain('Estado OIDC inválido');
  });

  it('troca code por token via PKCE e carrega sessao', async () => {
    sessionStorage.setItem('almox_pkce_state', 'st-1');
    sessionStorage.setItem('almox_pkce_verifier', 'ver-1');
    sessionStorage.setItem('almox_return_url', '/estoque');
    spyOn(router, 'navigateByUrl').and.resolveTo(true);

    const jwt = createJwt({
      preferred_username: 'almoxarife',
      organizacao_id: '0190f5a0-0000-7000-8000-000000000001',
      realm_access: { roles: ['ALMOXARIFE'] },
    });

    const promise = service.handleCallback('auth-code', 'st-1');

    const tokenReq = httpMock.expectOne(
      `${APP_ENVIRONMENT.keycloak.baseUrl}/realms/${APP_ENVIRONMENT.keycloak.realm}/protocol/openid-connect/token`,
    );
    expect(tokenReq.request.method).toBe('POST');
    tokenReq.flush({
      access_token: jwt,
      expires_in: 300,
      token_type: 'Bearer',
    });

    await Promise.resolve();

    const meReq = httpMock.expectOne(`${APP_ENVIRONMENT.apiBaseUrl}/auth/me`);
    meReq.flush({
      usuarioId: 'u-1',
      nomeUsuario: 'almoxarife',
      organizacaoId: '0190f5a0-0000-7000-8000-000000000001',
      perfis: ['ALMOXARIFE'],
    });

    const ok = await promise;
    expect(ok).toBeTrue();
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.hasAnyRole(['ALMOXARIFE'])).toBeTrue();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/estoque');
  });
});
