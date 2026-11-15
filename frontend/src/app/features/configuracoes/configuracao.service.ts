import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PerfilUsuario } from '../../core/models/auth.model';
import { ApiService } from '../../core/services/api.service';

export interface OrganizacaoInfo {
  nome: string;
  codigo: string;
  ativa: boolean;
}

export interface ConfiguracaoOrganizacao {
  diasAlertaValidade: number;
}

export interface Almoxarifado {
  id: string;
  codigo: string;
  nome: string;
  ativo: boolean;
}

export interface LocalArmazenamento {
  id: string;
  almoxarifadoId: string;
  codigo: string;
  descricao: string;
  ativo: boolean;
}

export interface Setor {
  codigo: string;
  nome: string;
  centroCusto?: string | null;
}

export interface UsuarioLocal {
  codigo: string;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  ativo: boolean;
}

export interface ProvisionamentoRequest {
  username: string;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  ativo: boolean;
  almoxarifados: string[] | null;
}

export interface ProvisionamentoResponse {
  id: string;
  status: string;
  keycloakId: string | null;
  erro: string | null;
}

@Injectable({ providedIn: 'root' })
export class ConfiguracaoService {
  private readonly api = inject(ApiService);

  organizacao(): Observable<OrganizacaoInfo> {
    return this.api.get<OrganizacaoInfo>('/organizacao');
  }

  atualizarOrganizacao(req: OrganizacaoInfo): Observable<void> {
    return this.api.put<void>('/organizacao', req);
  }

  configuracoes(): Observable<ConfiguracaoOrganizacao> {
    return this.api.get<ConfiguracaoOrganizacao>('/organizacao/configuracoes');
  }

  salvarConfiguracoes(diasAlertaValidade: number): Observable<void> {
    return this.api.put<void>('/organizacao/configuracoes', { diasAlertaValidade });
  }

  listarAlmoxarifados(pagina = 0, tamanho = 100): Observable<Almoxarifado[]> {
    return this.api.get<Almoxarifado[]>('/almoxarifados', { pagina, tamanho });
  }

  criarAlmoxarifado(codigo: string, nome: string, ativo = true): Observable<Almoxarifado> {
    return this.api.post<Almoxarifado>('/almoxarifados', { codigo, nome, ativo });
  }

  listarLocais(almoxarifadoId: string, pagina = 0, tamanho = 100): Observable<LocalArmazenamento[]> {
    return this.api.get<LocalArmazenamento[]>(`/almoxarifados/${almoxarifadoId}/locais`, {
      pagina,
      tamanho,
    });
  }

  criarLocal(
    almoxarifadoId: string,
    codigo: string,
    descricao: string,
    ativo = true,
  ): Observable<LocalArmazenamento> {
    return this.api.post<LocalArmazenamento>(`/almoxarifados/${almoxarifadoId}/locais`, {
      codigo,
      descricao,
      ativo,
    });
  }

  listarSetores(): Observable<Setor[]> {
    return this.api.get<Setor[]>('/setores');
  }

  salvarSetor(req: Setor): Observable<void> {
    return this.api.put<void>('/setores', req);
  }

  listarUsuarios(): Observable<UsuarioLocal[]> {
    return this.api.get<UsuarioLocal[]>('/usuarios');
  }

  provisionarUsuario(req: ProvisionamentoRequest): Observable<ProvisionamentoResponse> {
    return this.api.post<ProvisionamentoResponse>('/usuarios/provisionamentos', req, true);
  }

  buscarProvisionamento(id: string): Observable<ProvisionamentoResponse> {
    return this.api.get<ProvisionamentoResponse>(`/usuarios/provisionamentos/${id}`);
  }
}
