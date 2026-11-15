import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class RelatorioService {
  private readonly api = inject(ApiService);

  consumoPorSetor(
    inicio: string,
    fim: string,
    pagina = 0,
    tamanho = 50,
  ): Observable<Array<Record<string, unknown>>> {
    return this.api.get<Array<Record<string, unknown>>>('/relatorios/consumo-por-setor', {
      inicio,
      fim,
      pagina,
      tamanho,
    });
  }

  exportarConsumoCsv(inicio: string, fim: string): Observable<Blob> {
    return this.api.getBlob('/relatorios/consumo-por-setor/exportar', { inicio, fim });
  }
}
