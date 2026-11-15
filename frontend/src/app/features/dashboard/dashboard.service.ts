import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

export interface AlertaItem {
  chave: string;
  tipo: string;
  itemId: string;
  posicaoId: string | null;
  quantidade: number;
  limite: number | null;
  validade: string | null;
  revisao: string;
  almoxarifadoId: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);

  dashboard(): Observable<Record<string, unknown>> {
    return this.api.get<Record<string, unknown>>('/dashboard');
  }

  alertas(pagina = 0, tamanho = 50): Observable<AlertaItem[]> {
    return this.api.get<AlertaItem[]>('/alertas', { pagina, tamanho });
  }

  dispensar(chave: string, revisao: string): Observable<void> {
    return this.api.post<void>('/alertas/dispensar', { chave, revisao });
  }
}
