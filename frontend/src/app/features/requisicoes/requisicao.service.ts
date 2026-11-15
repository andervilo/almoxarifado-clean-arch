import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

export interface RequisicaoLinha {
  itemId: string;
  quantidade: number;
  atendida?: number;
  pendente?: number;
}

export interface Requisicao {
  id: string;
  localId: string;
  solicitanteId: string;
  status: string;
  erro: string | null;
  linhas: RequisicaoLinha[];
  setorCodigo: string | null;
  centroCusto: string | null;
}

export interface AlocacaoReserva {
  itemId: string;
  posicaoId: string;
  localId: string;
  lote: string | null;
  serie: string | null;
  validade: string | null;
  quantidade: number;
}

@Injectable({ providedIn: 'root' })
export class RequisicaoService {
  private readonly api = inject(ApiService);

  listar(pagina = 0, tamanho = 50): Observable<Requisicao[]> {
    return this.api.get<Requisicao[]>('/requisicoes', { pagina, tamanho });
  }

  buscar(id: string): Observable<Requisicao> {
    return this.api.get<Requisicao>(`/requisicoes/${id}`);
  }

  criar(
    localId: string,
    linhas: Array<{ itemId: string; quantidade: number }>,
    setorCodigo?: string | null,
  ): Observable<Requisicao> {
    return this.api.post<Requisicao>('/requisicoes', {
      localId,
      linhas,
      setorCodigo: setorCodigo || null,
    });
  }

  acao(
    id: string,
    acao: 'enviar' | 'separar' | 'entregar' | 'cancelar' | 'confirmar-recebimento',
  ): Observable<Requisicao> {
    return this.api.post<Requisicao>(`/requisicoes/${id}/${acao}`, null);
  }

  entregarParcial(
    id: string,
    linhas: Array<{ itemId: string; quantidade: number }>,
  ): Observable<Requisicao> {
    return this.api.post<Requisicao>(`/requisicoes/${id}/entregar`, { linhas }, true);
  }

  alocacoes(id: string): Observable<AlocacaoReserva[]> {
    return this.api.get<AlocacaoReserva[]>(`/requisicoes/${id}/alocacoes`);
  }
}
