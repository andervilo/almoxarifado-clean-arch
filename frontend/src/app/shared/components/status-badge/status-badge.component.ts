import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

type BadgeTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const TONE_MAP: Record<string, BadgeTone> = {
  RESERVADA: 'info',
  SEPARADA: 'info',
  ENTREGUE: 'success',
  FINALIZADA: 'success',
  CONCLUIDO: 'success',
  ATIVO: 'success',
  PARCIALMENTE_ENTREGUE: 'warning',
  CONTAGEM: 'warning',
  RESERVANDO: 'warning',
  ENTREGANDO: 'warning',
  FINALIZANDO: 'warning',
  REALOCANDO: 'warning',
  PENDENTE: 'warning',
  ATIVANDO: 'warning',
  VALIDADE_PROXIMA: 'warning',
  ESTOQUE_MINIMO: 'warning',
  FALHA_RESERVA: 'danger',
  FALHA: 'danger',
  CONFLITO: 'danger',
  CANCELADA: 'danger',
  CANCELADO: 'danger',
  LOTE_VENCIDO: 'danger',
  INATIVO: 'neutral',
  RASCUNHO: 'neutral',
  SUPERADO: 'neutral',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [attr.data-tone]="tone()">{{ label() }}</span>`,
  styles: `
    .badge {
      display: inline-flex;
      align-items: center;
      padding: 0.15rem 0.55rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 600;
      letter-spacing: 0.02em;
      background: #e2e8f0;
      color: #334155;
    }
    .badge[data-tone='success'] {
      background: var(--success-soft);
      color: var(--success);
    }
    .badge[data-tone='warning'] {
      background: var(--warning-soft);
      color: var(--warning);
    }
    .badge[data-tone='danger'] {
      background: var(--danger-soft);
      color: var(--danger);
    }
    .badge[data-tone='info'] {
      background: var(--info-soft);
      color: var(--info);
    }
  `,
})
export class StatusBadgeComponent {
  readonly status = input.required<string>();
  readonly label = computed(() => this.status().replace(/_/g, ' '));
  readonly tone = computed<BadgeTone>(() => TONE_MAP[this.status()] ?? 'neutral');
}
