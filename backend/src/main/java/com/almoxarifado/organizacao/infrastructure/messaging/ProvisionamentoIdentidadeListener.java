package com.almoxarifado.organizacao.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.organizacao.application.SincronizarIdentidade;
import com.almoxarifado.organizacao.events.ProvisionamentoIdentidadeSolicitado;
@Component
public class ProvisionamentoIdentidadeListener {
 private final SincronizarIdentidade casoDeUso;
 public ProvisionamentoIdentidadeListener(SincronizarIdentidade casoDeUso) {this.casoDeUso=casoDeUso;}
 @ApplicationModuleListener public void receber(ProvisionamentoIdentidadeSolicitado e) {casoDeUso.executar(e.organizacaoId(),e.provisionamentoId());}
}
