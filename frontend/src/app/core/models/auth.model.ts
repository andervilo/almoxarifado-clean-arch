export type PerfilUsuario =
  | 'ADMIN_PLATAFORMA'
  | 'ADMIN_ORGANIZACAO'
  | 'ALMOXARIFE'
  | 'REQUISITANTE'
  | 'GESTOR';

export interface SessaoUsuario {
  usuarioId: string;
  nomeUsuario: string;
  organizacaoId: string;
  perfis: PerfilUsuario[];
}

export interface TokenResponse {
  access_token: string;
  refresh_token?: string;
  id_token?: string;
  expires_in: number;
  token_type: string;
}

export interface ApiProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  codigo?: string;
  timestamp?: string;
  erros?: Array<{ campo: string; mensagem: string }>;
}
