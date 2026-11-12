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
 public record CriarRequest(@NotNull UUID localId) {}
 public record ContarRequest(@NotNull @PositiveOrZero @Digits(integer=15,fraction=4) BigDecimal quantidade) {}
 public record FinalizarRequest(@NotBlank @Size(max=500) String justificativa) {}
 public record ContagemResponse(UUID posicaoId,UUID itemId,String lote,String serie,BigDecimal sistema,BigDecimal contada,BigDecimal diferenca) {}
 public record Response(UUID id,UUID localId,String status,String erro,List<ContagemResponse> contagens) {}
 private Response resposta(InventarioRepository.Inventario i) { return new Response(i.id(),i.localId(),i.status(),i.erro(),i.contagens().stream().map(c->new ContagemResponse(c.posicaoId(),c.itemId(),c.lote(),c.serie(),c.sistema(),c.contada(),c.diferenca())).toList()); }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Response criar(@Valid @RequestBody CriarRequest req) { return resposta(service.criar(req.localId())); }
 @GetMapping("/{id}") public Response buscar(@PathVariable UUID id) { return resposta(service.buscar(id)); }
 @GetMapping public List<Response> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.listar(pagina,tamanho).stream().map(this::resposta).toList(); }
 @PutMapping("/{id}/contagens/{posicao}") public void contar(@PathVariable UUID id,@PathVariable UUID posicao,@Valid @RequestBody ContarRequest req) { service.contar(id,posicao,req.quantidade()); }
 @PostMapping("/{id}/finalizar") @ResponseStatus(HttpStatus.ACCEPTED) public void finalizar(@PathVariable UUID id,@Valid @RequestBody FinalizarRequest req) { service.finalizar(id,req.justificativa()); }
 @PostMapping("/{id}/cancelar") public void cancelar(@PathVariable UUID id) { service.cancelar(id); }
}
