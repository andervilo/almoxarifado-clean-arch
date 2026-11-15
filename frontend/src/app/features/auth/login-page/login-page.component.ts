import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AlertBannerComponent } from '../../../shared/components/alert-banner/alert-banner.component';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [AlertBannerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth-screen">
      <div class="card auth-card">
        <div class="auth-header">
          <h1>Sistema de Almoxarifado</h1>
          <p class="muted">
            Autenticação centralizada via Keycloak (OIDC / Authorization Code + PKCE).
          </p>
        </div>

        <app-alert-banner kind="error" [message]="auth.error()" />

        @if (auth.loading()) {
          <p class="muted">Validando sessão com o Keycloak...</p>
        } @else {
          <button type="button" class="btn btn-primary login-btn" (click)="entrar()">
            Entrar com Keycloak
          </button>
        }
      </div>
    </div>
  `,
  styles: `
    .auth-screen {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 1.5rem;
      background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
    }
    .auth-card {
      width: 100%;
      max-width: 420px;
      display: flex;
      flex-direction: column;
      gap: 1.15rem;
      padding: 2rem;
    }
    .auth-header h1 {
      font-size: 1.35rem;
      font-weight: 700;
      margin-bottom: 0.35rem;
    }
    .login-btn {
      width: 100%;
      padding: 0.7rem 1rem;
    }
  `,
})
export class LoginPageComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const code = params.get('code');
    const state = params.get('state');

    if (code && state) {
      void this.auth.handleCallback(code, state);
      return;
    }

    if (this.auth.isAuthenticated()) {
      const returnUrl = params.get('returnUrl') || '/dashboard';
      void this.router.navigateByUrl(returnUrl);
    }
  }

  entrar(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
    void this.auth.login(returnUrl);
  }
}
