import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty-state">
      <p>{{ message() }}</p>
    </div>
  `,
  styles: `
    .empty-state {
      padding: 2rem 1rem;
      text-align: center;
      color: var(--text-muted);
      border: 1px dashed var(--border-strong);
      border-radius: var(--radius);
      background: var(--surface-alt);
    }
  `,
})
export class EmptyStateComponent {
  readonly message = input<string>('Nenhum registro encontrado.');
}
