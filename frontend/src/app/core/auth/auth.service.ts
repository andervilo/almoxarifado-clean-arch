import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { APP_ENVIRONMENT } from '../config/app-environment';
import { PerfilUsuario, SessaoUsuario, TokenResponse } from '../models/auth.model';
import { generateCodeChallenge, generateRandomString } from './pkce.util';

const STORAGE_ACCESS_TOKEN = 'almox_access_token';
const STORAGE_REFRESH_TOKEN = 'almox_refresh_token';
const STORAGE_ID_TOKEN = 'almox_id_token';
const STORAGE_EXPIRES_AT = 'almox_expires_at';
const STORAGE_PKCE_VERIFIER = 'almox_pkce_verifier';
const STORAGE_PKCE_STATE = 'almox_pkce_state';
const STORAGE_RETURN_URL = 'almox_return_url';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _accessToken = signal<string | null>(this.readStorage(STORAGE_ACCESS_TOKEN));
  private readonly _expiresAt = signal<number>(Number(this.readStorage(STORAGE_EXPIRES_AT) ?? 0));
  private readonly _session = signal<SessaoUsuario | null>(null);
  private readonly _loading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  readonly accessToken = computed(() => this._accessToken());
  readonly session = computed(() => this._session());
  readonly loading = computed(() => this._loading());
  readonly error = computed(() => this._error());
  readonly isAuthenticated = computed(() => {
    const token = this._accessToken();
    return Boolean(token) && Date.now() < this._expiresAt() - 5_000;
  });

  readonly roles = computed<PerfilUsuario[]>(() => {
    const sessao = this._session();
    if (sessao?.perfis?.length) {
      return sessao.perfis;
    }
    return this.extractRolesFromJwt(this._accessToken());
  });

  readonly organizacaoId = computed<string | null>(() => {
    return this._session()?.organizacaoId ?? this.extractClaimFromJwt<string>(this._accessToken(), 'organizacao_id');
  });

  readonly nomeUsuario = computed<string>(() => {
    return (
      this._session()?.nomeUsuario ??
      this.extractClaimFromJwt<string>(this._accessToken(), 'preferred_username') ??
      'Usuário'
    );
  });

  private get realmBaseUrl(): string {
    const { baseUrl, realm } = APP_ENVIRONMENT.keycloak;
    return `${baseUrl}/realms/${realm}/protocol/openid-connect`;
  }

  hasAnyRole(allowed: readonly PerfilUsuario[]): boolean {
    if (!allowed.length) {
      return true;
    }
    const current = this.roles();
    return allowed.some((role) => current.includes(role));
  }

  async login(returnUrl = '/dashboard'): Promise<void> {
    this._error.set(null);
    const verifier = generateRandomString(48);
    const state = generateRandomString(24);
    const challenge = await generateCodeChallenge(verifier);

    sessionStorage.setItem(STORAGE_PKCE_VERIFIER, verifier);
    sessionStorage.setItem(STORAGE_PKCE_STATE, state);
    sessionStorage.setItem(STORAGE_RETURN_URL, returnUrl);

    const params = new URLSearchParams({
      client_id: APP_ENVIRONMENT.keycloak.clientId,
      redirect_uri: APP_ENVIRONMENT.keycloak.redirectUri,
      response_type: 'code',
      scope: APP_ENVIRONMENT.keycloak.scope,
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    });

    this.redirectTo(`${this.realmBaseUrl}/auth?${params.toString()}`);
  }

  async handleCallback(code: string, state: string): Promise<boolean> {
    const expectedState = sessionStorage.getItem(STORAGE_PKCE_STATE);
    const verifier = sessionStorage.getItem(STORAGE_PKCE_VERIFIER);
    const returnUrl = sessionStorage.getItem(STORAGE_RETURN_URL) || '/dashboard';

    sessionStorage.removeItem(STORAGE_PKCE_STATE);
    sessionStorage.removeItem(STORAGE_PKCE_VERIFIER);
    sessionStorage.removeItem(STORAGE_RETURN_URL);

    if (!expectedState || !verifier || state !== expectedState) {
      this._error.set('Estado OIDC inválido ou expirado. Tente entrar novamente.');
      return false;
    }

    this._loading.set(true);
    this._error.set(null);
    try {
      const body = new HttpParams()
        .set('grant_type', 'authorization_code')
        .set('client_id', APP_ENVIRONMENT.keycloak.clientId)
        .set('code', code)
        .set('redirect_uri', APP_ENVIRONMENT.keycloak.redirectUri)
        .set('code_verifier', verifier);

      const tokens = await firstValueFrom(
        this.http.post<TokenResponse>(`${this.realmBaseUrl}/token`, body.toString(), {
          headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
        }),
      );

      this.storeTokens(tokens);
      await this.loadSessionMe();
      await this.router.navigateByUrl(returnUrl);
      return true;
    } catch {
      this.clearTokens();
      this._error.set('Falha ao concluir autenticação com o Keycloak.');
      return false;
    } finally {
      this._loading.set(false);
    }
  }

  async loadSessionMe(): Promise<SessaoUsuario | null> {
    if (!this.isAuthenticated()) {
      return null;
    }
    try {
      const sessao = await firstValueFrom(
        this.http.get<SessaoUsuario>(`${APP_ENVIRONMENT.apiBaseUrl}/auth/me`),
      );
      this._session.set(sessao);
      return sessao;
    } catch {
      return null;
    }
  }

  async tryRefreshToken(): Promise<boolean> {
    const refreshToken = this.readStorage(STORAGE_REFRESH_TOKEN);
    if (!refreshToken) {
      return false;
    }

    try {
      const body = new HttpParams()
        .set('grant_type', 'refresh_token')
        .set('client_id', APP_ENVIRONMENT.keycloak.clientId)
        .set('refresh_token', refreshToken);

      const tokens = await firstValueFrom(
        this.http.post<TokenResponse>(`${this.realmBaseUrl}/token`, body.toString(), {
          headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
        }),
      );
      this.storeTokens(tokens);
      return true;
    } catch {
      this.clearTokens();
      return false;
    }
  }

  logout(): void {
    const idToken = this.readStorage(STORAGE_ID_TOKEN);
    this.clearTokens();

    const params = new URLSearchParams({
      client_id: APP_ENVIRONMENT.keycloak.clientId,
      post_logout_redirect_uri: APP_ENVIRONMENT.keycloak.postLogoutRedirectUri,
    });
    if (idToken) {
      params.set('id_token_hint', idToken);
    }
    this.redirectTo(`${this.realmBaseUrl}/logout?${params.toString()}`);
  }

  clearSessionLocal(): void {
    this.clearTokens();
  }

  protected redirectTo(url: string): void {
    window.location.assign(url);
  }

  private storeTokens(tokens: TokenResponse): void {
    const expiresAt = Date.now() + tokens.expires_in * 1000;
    sessionStorage.setItem(STORAGE_ACCESS_TOKEN, tokens.access_token);
    sessionStorage.setItem(STORAGE_EXPIRES_AT, String(expiresAt));
    if (tokens.refresh_token) {
      sessionStorage.setItem(STORAGE_REFRESH_TOKEN, tokens.refresh_token);
    }
    if (tokens.id_token) {
      sessionStorage.setItem(STORAGE_ID_TOKEN, tokens.id_token);
    }
    this._accessToken.set(tokens.access_token);
    this._expiresAt.set(expiresAt);
  }

  private clearTokens(): void {
    sessionStorage.removeItem(STORAGE_ACCESS_TOKEN);
    sessionStorage.removeItem(STORAGE_REFRESH_TOKEN);
    sessionStorage.removeItem(STORAGE_ID_TOKEN);
    sessionStorage.removeItem(STORAGE_EXPIRES_AT);
    this._accessToken.set(null);
    this._expiresAt.set(0);
    this._session.set(null);
  }

  private readStorage(key: string): string | null {
    return typeof sessionStorage !== 'undefined' ? sessionStorage.getItem(key) : null;
  }

  private extractRolesFromJwt(token: string | null): PerfilUsuario[] {
    const payload = this.decodeJwtPayload(token);
    const roles = (payload?.['realm_access'] as { roles?: string[] } | undefined)?.roles ?? [];
    const valid: PerfilUsuario[] = [
      'ADMIN_PLATAFORMA',
      'ADMIN_ORGANIZACAO',
      'ALMOXARIFE',
      'REQUISITANTE',
      'GESTOR',
    ];
    return roles.filter((r): r is PerfilUsuario => valid.includes(r as PerfilUsuario));
  }

  private extractClaimFromJwt<T>(token: string | null, claim: string): T | null {
    const payload = this.decodeJwtPayload(token);
    return (payload?.[claim] as T | undefined) ?? null;
  }

  private decodeJwtPayload(token: string | null): Record<string, unknown> | null {
    if (!token) {
      return null;
    }
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    try {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
      return JSON.parse(atob(padded)) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}
