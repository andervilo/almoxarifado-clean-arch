package com.almoxarifado.catalogo.infrastructure.web;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.catalogo.application.RepublicarCatalogo;
@RestController @RequestMapping("/api/v1/catalogo/sincronizacoes")
public class SincronizacaoCatalogoController {
 private final RepublicarCatalogo casoDeUso;
 public SincronizacaoCatalogoController(RepublicarCatalogo casoDeUso) { this.casoDeUso=casoDeUso; }
 @PostMapping("/{recurso}")
 public RepublicarCatalogo.Resultado executar(@PathVariable String recurso,@RequestParam(required=false) UUID apos,@RequestParam(defaultValue="100") int tamanho) {
  return casoDeUso.executar(recurso,apos,tamanho);
 }
}
