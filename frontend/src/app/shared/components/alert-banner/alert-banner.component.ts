import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-alert-banner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (message()) {
      <div class="banner" [attr.data-kind]="kind()" role="status">
        {{ message() }}
      </div>
    }
  `,
  styles: `
    .banner {
      padding: 0.7rem 0.95rem;
      border-radius: 6px;
      font-size: 0.875rem;
      border: 1px solid transparent;
    }
    .banner[data-kind='error'] {
      background: var(--danger-soft);
      color: var(--danger);
      border-color: #fecaca;
    }
    .banner[data-kind='success'] {
      background: var(--success-soft);
      color: var(--success);
      border-color: #bbf7d0;
    }
    .banner[data-kind='info'] {
      background: var(--info-soft);
      color: var(--info);
      border-color: #bae6fd;
    }
  `,
})
export class AlertBannerComponent {
  readonly kind = input<'error' | 'success' | 'info'>('info');
  readonly message = input<string | null>(null);
}
