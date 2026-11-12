package com.almoxarifado.organizacao.infrastructure.web;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.organizacao.application.RepublicarOrganizacao;
@RestController @RequestMapping("/api/v1/organizacao/sincronizacoes")
public class SincronizacaoOrganizacaoController {
 private final RepublicarOrganizacao casoDeUso;
 public SincronizacaoOrganizacaoController(RepublicarOrganizacao casoDeUso) { this.casoDeUso=casoDeUso; }
 @PostMapping("/{recurso}")
 public RepublicarOrganizacao.Resultado executar(@PathVariable String recurso,@RequestParam(required=false) UUID apos,@RequestParam(defaultValue="100") int tamanho) {
  return casoDeUso.executar(recurso,apos,tamanho);
 }
}
