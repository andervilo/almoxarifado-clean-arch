import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

export type TipoRastreabilidade = 'NENHUM' | 'LOTE' | 'LOTE_VALIDADE' | 'NUMERO_SERIE';

export interface CadastroAuxiliar {
  id: string;
  codigo: string;
  nome: string;
  ativo: boolean;
}

export interface CadastroAuxiliarRequest {
  codigo: string;
  nome: string;
  ativo: boolean;
}

export interface ItemCatalogo {
  id: string;
  codigo: string;
  descricao: string;
  categoriaId: string;
  unidadeMedidaId: string;
  rastreabilidade: TipoRastreabilidade;
  estoqueMinimo: number;
  estoqueMaximo: number;
  ativo: boolean;
}

export interface ItemCatalogoRequest {
  codigo: string;
  descricao: string;
  categoriaId: string;
  unidadeMedidaId: string;
  rastreabilidade: TipoRastreabilidade;
  estoqueMinimo: number;
  estoqueMaximo: number;
  ativo: boolean;
}

export interface ParametrosReposicao {
  itemId: string;
  almoxarifadoId: string;
  minimo: number;
  maximo: number;
}

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly api = inject(ApiService);

  listarCategorias(pagina = 0, tamanho = 100): Observable<CadastroAuxiliar[]> {
    return this.api.get<CadastroAuxiliar[]>('/catalogo/categorias', { pagina, tamanho });
  }

  criarCategoria(req: CadastroAuxiliarRequest): Observable<CadastroAuxiliar> {
    return this.api.post<CadastroAuxiliar>('/catalogo/categorias', req);
  }

  listarUnidades(pagina = 0, tamanho = 100): Observable<CadastroAuxiliar[]> {
    return this.api.get<CadastroAuxiliar[]>('/catalogo/unidades-medida', { pagina, tamanho });
  }

  criarUnidade(req: CadastroAuxiliarRequest): Observable<CadastroAuxiliar> {
    return this.api.post<CadastroAuxiliar>('/catalogo/unidades-medida', req);
  }

  listarFornecedores(pagina = 0, tamanho = 100): Observable<CadastroAuxiliar[]> {
    return this.api.get<CadastroAuxiliar[]>('/catalogo/fornecedores', { pagina, tamanho });
  }

  criarFornecedor(req: CadastroAuxiliarRequest): Observable<CadastroAuxiliar> {
    return this.api.post<CadastroAuxiliar>('/catalogo/fornecedores', req);
  }

  listarItens(pagina = 0, tamanho = 100): Observable<ItemCatalogo[]> {
    return this.api.get<ItemCatalogo[]>('/catalogo/itens', { pagina, tamanho });
  }

  criarItem(req: ItemCatalogoRequest): Observable<ItemCatalogo> {
    return this.api.post<ItemCatalogo>('/catalogo/itens', req);
  }

  atualizarItem(id: string, req: ItemCatalogoRequest): Observable<ItemCatalogo> {
    return this.api.put<ItemCatalogo>(`/catalogo/itens/${id}`, req);
  }

  buscarParametrosReposicao(itemId: string, almoxarifadoId: string): Observable<ParametrosReposicao> {
    return this.api.get<ParametrosReposicao>(
      `/catalogo/itens/${itemId}/parametros-reposicao/${almoxarifadoId}`,
    );
  }

  salvarParametrosReposicao(
    itemId: string,
    almoxarifadoId: string,
    minimo: number,
    maximo: number,
  ): Observable<ParametrosReposicao> {
    return this.api.put<ParametrosReposicao>(
      `/catalogo/itens/${itemId}/parametros-reposicao/${almoxarifadoId}`,
      { minimo, maximo },
    );
  }
}
