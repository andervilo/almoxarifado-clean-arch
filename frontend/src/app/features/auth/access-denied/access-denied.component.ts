import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="denied-wrap">
      <div class="card denied-card">
        <h1>Acesso negado</h1>
        <p class="muted">
          O seu perfil atual ({{ auth.roles().join(', ') || 'nenhum' }}) não possui permissão para acessar esta área.
        </p>
        <div class="actions-row">
          <a class="btn btn-primary" routerLink="/requisicoes">Ir para área permitida</a>
          <button type="button" class="btn btn-secondary" (click)="auth.logout()">Trocar usuário</button>
        </div>
      </div>
    </div>
  `,
  styles: `
    .denied-wrap {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 1.5rem;
    }
    .denied-card {
      max-width: 460px;
      display: flex;
      flex-direction: column;
      gap: 1rem;
      padding: 2rem;
    }
  `,
})
export class AccessDeniedComponent {
  protected readonly auth = inject(AuthService);
}
