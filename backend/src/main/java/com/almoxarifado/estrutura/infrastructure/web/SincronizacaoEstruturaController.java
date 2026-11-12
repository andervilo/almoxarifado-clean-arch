package com.almoxarifado.estrutura.infrastructure.web;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.estrutura.application.RepublicarEstrutura;
@RestController @RequestMapping("/api/v1/estrutura/sincronizacoes")
public class SincronizacaoEstruturaController {
 private final RepublicarEstrutura casoDeUso;
 public SincronizacaoEstruturaController(RepublicarEstrutura casoDeUso) { this.casoDeUso=casoDeUso; }
 @PostMapping("/{recurso}")
 public RepublicarEstrutura.Resultado executar(@PathVariable String recurso,@RequestParam(required=false) UUID apos,@RequestParam(defaultValue="100") int tamanho) {
  return casoDeUso.executar(recurso,apos,tamanho);
 }
}
