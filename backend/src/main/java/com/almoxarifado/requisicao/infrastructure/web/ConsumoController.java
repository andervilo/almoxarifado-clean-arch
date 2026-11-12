package com.almoxarifado.requisicao.infrastructure.web;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import com.almoxarifado.requisicao.application.RequisicaoService;
@RestController @RequestMapping("/api/v1/relatorios/consumo-por-setor")
public class ConsumoController {
 private final RequisicaoService service;
 public ConsumoController(RequisicaoService service) {this.service=service;}
 @GetMapping public List<Map<String,Object>> consultar(@RequestParam LocalDate inicio,@RequestParam LocalDate fim,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) {return service.consumo(inicio,fim,pagina,tamanho);}
 @GetMapping(value="/exportar",produces="text/csv")
 public ResponseEntity<String> exportar(@RequestParam LocalDate inicio,@RequestParam LocalDate fim) {
  return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=consumo-por-setor.csv").body(service.exportarConsumo(inicio,fim));
 }
}
