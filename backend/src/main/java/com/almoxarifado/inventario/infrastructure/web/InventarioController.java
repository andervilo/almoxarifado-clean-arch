package com.almoxarifado.inventario.infrastructure.web;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.inventario.application.*;
@RestController @RequestMapping("/api/v1/inventarios")
public class InventarioController {
 private final InventarioService service;
 public InventarioController(InventarioService service) { this.service=service; }
 public record CriarRequest(@NotNull UUID localId,boolean contagemCega) {}
 public record ContarRequest(@NotNull @PositiveOrZero @Digits(integer=15,fraction=4) BigDecimal quantidade) {}
 public record FinalizarRequest(@NotBlank @Size(max=500) String justificativa) {}
 public record ContagemResponse(UUID posicaoId,UUID itemId,String lote,String serie,BigDecimal sistema,BigDecimal contada,BigDecimal diferenca) {}
 public record Response(UUID id,UUID localId,String status,String erro,List<ContagemResponse> contagens,boolean contagemCega) {}
 private Response resposta(InventarioRepository.Inventario i) {
  boolean ocultar=i.contagemCega() && !Set.of("CONCLUIDO","CANCELADO").contains(i.status());
  return new Response(i.id(),i.localId(),i.status(),i.erro(),i.contagens().stream().map(c->new ContagemResponse(c.posicaoId(),c.itemId(),c.lote(),c.serie(),ocultar?null:c.sistema(),c.contada(),ocultar?null:c.diferenca())).toList(),i.contagemCega());
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Response criar(@Valid @RequestBody CriarRequest req) { return resposta(service.criar(req.localId(),req.contagemCega())); }
 public record NovaContagemRequest(@NotNull UUID itemId,@Size(max=120) String lote,@Size(max=120) String serie,java.time.LocalDate validade,
  @NotNull @Positive @Digits(integer=15,fraction=4) BigDecimal quantidade,@NotNull @PositiveOrZero @Digits(integer=13,fraction=6) BigDecimal custo) {}
 @PostMapping("/{id}/contagens") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,UUID> adicionar(@PathVariable UUID id,@Valid @RequestBody NovaContagemRequest req) {return Map.of("posicaoId",service.adicionar(id,req.itemId(),req.lote(),req.serie(),req.validade(),req.quantidade(),req.custo()));}
 @GetMapping("/{id}") public Response buscar(@PathVariable UUID id) { return resposta(service.buscar(id)); }
 @GetMapping public List<Response> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.listar(pagina,tamanho).stream().map(this::resposta).toList(); }
 @PutMapping("/{id}/contagens/{posicao}") public void contar(@PathVariable UUID id,@PathVariable UUID posicao,@Valid @RequestBody ContarRequest req) { service.contar(id,posicao,req.quantidade()); }
 @PostMapping("/{id}/finalizar") @ResponseStatus(HttpStatus.ACCEPTED) public void finalizar(@PathVariable UUID id,@Valid @RequestBody FinalizarRequest req) { service.finalizar(id,req.justificativa()); }
 @PostMapping("/{id}/cancelar") public void cancelar(@PathVariable UUID id) { service.cancelar(id); }
}
