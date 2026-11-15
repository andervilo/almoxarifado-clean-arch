import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

export type TipoOperacaoEstoque = 'entradas' | 'saidas' | 'transferencias' | 'ajustes' | 'estornos';

export interface MovimentoRequest {
  itemId: string;
  origemId?: string | null;
  destinoId?: string | null;
  quantidade?: number | null;
  lote?: string | null;
  serie?: string | null;
  validade?: string | null;
  custo?: number | null;
  motivo: string;
  referenciaId?: string | null;
}

export interface SaldoPosicao {
  id: string;
  itemId: string;
  localId: string;
  lote: string | null;
  serie: string | null;
  validade: string | null;
  quantidade: number;
  reservado: number;
  disponivel: number;
  custo: number | null;
}

@Injectable({ providedIn: 'root' })
export class EstoqueService {
  private readonly api = inject(ApiService);

  movimentar(tipo: TipoOperacaoEstoque, req: MovimentoRequest): Observable<{ id: string }> {
    return this.api.post<{ id: string }>(`/estoque/movimentacoes/${tipo}`, req, true);
  }

  saldos(itemId: string): Observable<SaldoPosicao[]> {
    return this.api.get<SaldoPosicao[]>('/estoque/saldos', { itemId });
  }

  custos(itemId: string): Observable<Array<Record<string, unknown>>> {
    return this.api.get<Array<Record<string, unknown>>>('/estoque/custos', { itemId });
  }

  historico(itemId: string, pagina = 0, tamanho = 30): Observable<Array<Record<string, unknown>>> {
    return this.api.get<Array<Record<string, unknown>>>('/estoque/movimentacoes', {
      itemId,
      pagina,
      tamanho,
    });
  }

  kardex(
    itemId: string,
    lote?: string,
    serie?: string,
    pagina = 0,
    tamanho = 50,
  ): Observable<Array<Record<string, unknown>>> {
    return this.api.get<Array<Record<string, unknown>>>('/estoque/kardex', {
      itemId,
      lote,
      serie,
      pagina,
      tamanho,
    });
  }
}
