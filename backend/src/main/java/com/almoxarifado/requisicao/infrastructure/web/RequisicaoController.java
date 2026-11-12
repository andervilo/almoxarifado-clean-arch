package com.almoxarifado.requisicao.infrastructure.web;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.requisicao.domain.Requisicao;
import com.almoxarifado.requisicao.application.RequisicaoService;
@RestController @RequestMapping("/api/v1/requisicoes")
public class RequisicaoController {
 private final RequisicaoService service;
 public RequisicaoController(RequisicaoService service) { this.service=service; }
 public record LinhaRequest(@NotNull UUID itemId,@NotNull @Positive @Digits(integer=15,fraction=4) BigDecimal quantidade) {}
 public record CriarRequest(@NotNull UUID localId,@NotEmpty @Size(max=100) List<@Valid LinhaRequest> linhas,@Size(max=120) String setorCodigo) {}
 public record LinhaResponse(UUID itemId,BigDecimal quantidade,BigDecimal atendida,BigDecimal pendente) {}
 public record EntregaRequest(@NotEmpty @Size(max=100) List<@Valid LinhaRequest> linhas) {}
 public record AlocacaoRequest(@NotNull UUID posicaoId,@NotNull @Positive @Digits(integer=15,fraction=4) BigDecimal quantidade) {}
 public record RealocarRequest(@NotBlank @Size(max=500) String motivo,@NotEmpty @Size(max=100) List<@Valid AlocacaoRequest> alocacoes) {}
 public record AlocacaoResponse(UUID itemId,UUID posicaoId,UUID localId,String lote,String serie,java.time.LocalDate validade,BigDecimal quantidade) {}
 @GetMapping("/{id}/alocacoes") public List<AlocacaoResponse> alocacoes(@PathVariable UUID id) {
  return service.alocacoes(id).stream().map(a->new AlocacaoResponse(a.itemId(),a.posicaoId(),a.localId(),a.lote(),a.serie(),a.validade(),a.quantidade())).toList();
 }
 @PutMapping("/{id}/alocacoes/{item}") @ResponseStatus(HttpStatus.ACCEPTED)
 public Response realocar(@PathVariable UUID id,@PathVariable UUID item,@RequestHeader("Idempotency-Key") UUID chave,@Valid @RequestBody RealocarRequest req) {
  return resposta(service.realocar(id,item,req.alocacoes().stream().map(a->new com.almoxarifado.requisicao.domain.AlocacaoReserva(a.posicaoId(),a.quantidade())).toList(),req.motivo(),chave));
 }
 public record Response(UUID id,UUID localId,String solicitanteId,String status,String erro,List<LinhaResponse> linhas,String setorCodigo,String centroCusto) {}
 private Response resposta(Requisicao r) { return new Response(r.id(),r.localId(),r.solicitanteId(),r.status(),r.erro(),r.linhas().stream().map(l->new LinhaResponse(l.itemId(),l.quantidade(),l.atendida(),l.pendente())).toList(),r.setorCodigo(),r.centroCusto()); }
 @PostMapping("/{id}/entregar") @ResponseStatus(HttpStatus.ACCEPTED)
 public Response entregar(@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody(required=false) EntregaRequest req) {
  return resposta(service.entregar(id,req==null?null:req.linhas().stream().map(l->new Requisicao.Linha(l.itemId(),l.quantidade())).toList(),chave));
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Response criar(@Valid @RequestBody CriarRequest req) { return resposta(service.criar(req.localId(),req.linhas().stream().map(l->new Requisicao.Linha(l.itemId(),l.quantidade())).toList(),req.setorCodigo())); }
 @GetMapping("/{id}") public Response buscar(@PathVariable UUID id) { return resposta(service.buscar(id)); }
 @PutMapping("/{id}") public Response editar(@PathVariable UUID id,@Valid @RequestBody CriarRequest req) {
  return resposta(service.editar(id,req.localId(),req.linhas().stream().map(l->new Requisicao.Linha(l.itemId(),l.quantidade())).toList(),req.setorCodigo()));
 }
 @GetMapping public List<Response> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.listar(pagina,tamanho).stream().map(this::resposta).toList(); }
 @PostMapping("/{id}/{acao}") @ResponseStatus(HttpStatus.ACCEPTED) public Response acao(@PathVariable UUID id,@PathVariable String acao) {
  return resposta(switch(acao) {
   case "enviar" -> service.enviar(id);case "separar" -> service.separar(id);case "entregar" -> service.entregar(id);
   case "cancelar" -> service.cancelar(id);case "confirmar-recebimento" -> service.confirmar(id);
   default -> throw new IllegalArgumentException("Acao invalida");
  });
 }
}
