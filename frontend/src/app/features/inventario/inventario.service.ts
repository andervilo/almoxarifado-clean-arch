import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

export interface ContagemInventario {
  posicaoId: string;
  itemId: string;
  lote: string | null;
  serie: string | null;
  sistema: number | null;
  contada: number | null;
  diferenca: number | null;
}

export interface Inventario {
  id: string;
  localId: string;
  status: string;
  erro: string | null;
  contagens: ContagemInventario[];
  contagemCega: boolean;
}

export interface NovaContagemRequest {
  itemId: string;
  lote?: string | null;
  serie?: string | null;
  validade?: string | null;
  quantidade: number;
  custo: number;
}

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly api = inject(ApiService);

  listar(pagina = 0, tamanho = 50): Observable<Inventario[]> {
    return this.api.get<Inventario[]>('/inventarios', { pagina, tamanho });
  }

  buscar(id: string): Observable<Inventario> {
    return this.api.get<Inventario>(`/inventarios/${id}`);
  }

  criar(localId: string, contagemCega: boolean): Observable<Inventario> {
    return this.api.post<Inventario>('/inventarios', { localId, contagemCega });
  }

  contar(id: string, posicaoId: string, quantidade: number): Observable<void> {
    return this.api.put<void>(`/inventarios/${id}/contagens/${posicaoId}`, { quantidade });
  }

  adicionarPosicao(id: string, req: NovaContagemRequest): Observable<{ posicaoId: string }> {
    return this.api.post<{ posicaoId: string }>(`/inventarios/${id}/contagens`, req);
  }

  finalizar(id: string, justificativa: string): Observable<void> {
    return this.api.post<void>(`/inventarios/${id}/finalizar`, { justificativa });
  }

  cancelar(id: string): Observable<void> {
    return this.api.post<void>(`/inventarios/${id}/cancelar`, null);
  }
}
