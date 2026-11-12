package com.almoxarifado.estoque.infrastructure.web;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.estoque.application.*;
@RestController
@RequestMapping("/api/v1/estoque")
public class EstoqueController {
 private final EstoqueService service;
 public EstoqueController(EstoqueService service) { this.service=service; }
 public record MovimentoRequest(@NotNull UUID itemId,UUID origemId,UUID destinoId,
  @Digits(integer=15,fraction=4) BigDecimal quantidade,@Size(max=120) String lote,@Size(max=120) String serie,
  LocalDate validade,@Digits(integer=13,fraction=6) BigDecimal custo,@NotBlank @Size(max=500) String motivo,UUID referenciaId) {}
 public record MovimentoResponse(UUID id) {}
 public record SaldoResponse(UUID id,UUID itemId,UUID localId,String lote,String serie,LocalDate validade,
  BigDecimal quantidade,BigDecimal reservado,BigDecimal disponivel,BigDecimal custo) {}
 @PostMapping("/movimentacoes/{tipo}") @ResponseStatus(HttpStatus.CREATED)
 public MovimentoResponse movimentar(@PathVariable String tipo,@RequestHeader("Idempotency-Key") UUID id,@Valid @RequestBody MovimentoRequest req) {
  String operacao=switch(tipo) {
   case "entradas" -> "ENTRADA"; case "saidas" -> "SAIDA"; case "transferencias" -> "TRANSFERENCIA";
   case "ajustes" -> "AJUSTE"; case "estornos" -> "ESTORNO";
   default -> throw new IllegalArgumentException("Tipo invalido");
  };
  return new MovimentoResponse(service.movimentar(id,new ComandoEstoque(operacao,req.itemId(),req.origemId(),req.destinoId(),req.quantidade(),req.lote(),req.serie(),req.validade(),req.custo(),req.motivo(),req.referenciaId())));
 }
 @GetMapping("/saldos")
 public List<SaldoResponse> saldos(@RequestParam UUID itemId) {
  return service.saldos(itemId).stream().map(p->new SaldoResponse(p.id(),p.itemId(),p.localId(),p.lote(),p.serie(),p.validade(),p.quantidade(),p.reservado(),p.disponivel(),p.custo())).toList();
 }
 @GetMapping("/movimentacoes") public List<Map<String,Object>> historico(@RequestParam UUID itemId,
  @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.historico(itemId,pagina,tamanho); }
 @GetMapping("/kardex") public List<Map<String,Object>> kardex(@RequestParam UUID itemId,@RequestParam(required=false) String lote,
  @RequestParam(required=false) String serie,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) {
  return service.rastrear(itemId,lote,serie,pagina,tamanho);
 }
 @GetMapping("/custos") public List<Map<String,Object>> custos(@RequestParam UUID itemId) { return service.custos(itemId); }
}
