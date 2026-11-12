package com.almoxarifado.estoque.infrastructure.web;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.estoque.application.RepublicarEstoque;
@RestController @RequestMapping("/api/v1/estoque/sincronizacoes")
public class SincronizacaoEstoqueController {
 private final RepublicarEstoque casoDeUso;
 public SincronizacaoEstoqueController(RepublicarEstoque casoDeUso) { this.casoDeUso=casoDeUso; }
 @PostMapping("/{recurso}")
 public RepublicarEstoque.Resultado executar(@PathVariable String recurso,@RequestParam(required=false) UUID apos,@RequestParam(defaultValue="100") int tamanho) {
  return casoDeUso.executar(recurso,apos,tamanho);
 }
}
