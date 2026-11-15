import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblemDetail } from '../models/auth.model';

export function extractApiErrorMessage(err: unknown, fallback = 'Não foi possível concluir a operação.'): string {
  if (!(err instanceof HttpErrorResponse)) {
    return fallback;
  }
  if (err.status === 0) {
    return 'Sem conexão com a API do backend (verifique se o servidor está ativo).';
  }
  const body = err.error as ApiProblemDetail | string | null;
  if (body && typeof body === 'object') {
    const fieldErrors = body.erros?.map((e) => `${e.campo}: ${e.mensagem}`).join('; ');
    if (fieldErrors) {
      return `${body.detail ?? body.title ?? 'Dados inválidos'} (${fieldErrors})`;
    }
    if (body.detail) {
      return body.codigo ? `[${body.codigo}] ${body.detail}` : body.detail;
    }
    if (body.title) {
      return body.title;
    }
  }
  if (typeof body === 'string' && body.trim().length > 0) {
    return body;
  }
  return `${fallback} (HTTP ${err.status})`;
}
