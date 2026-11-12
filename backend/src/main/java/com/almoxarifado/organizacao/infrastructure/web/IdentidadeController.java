package com.almoxarifado.organizacao.infrastructure.web;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.organizacao.domain.*;
import com.almoxarifado.organizacao.application.ProvisionarIdentidade;
@RestController @RequestMapping("/api/v1/usuarios")
public class IdentidadeController {
 private final ProvisionarIdentidade casoDeUso;
 public IdentidadeController(ProvisionarIdentidade casoDeUso) {this.casoDeUso=casoDeUso;}
 public record ContaRequest(@NotBlank @Pattern(regexp="[a-zA-Z0-9._-]{3,120}") String username,@NotBlank @Size(max=255) String nome,
  @NotBlank @Email @Size(max=255) String email,@NotNull PerfilUsuario perfil,boolean ativo,@Size(max=1000) Set<@NotNull UUID> almoxarifados) {}
 public record Response(UUID id,String status,UUID keycloakId,String erro) {}
 private Response resposta(ProvisionamentoIdentidade p) {return new Response(p.id(),p.status(),p.keycloakId(),p.erro());}
 private DadosConta dados(UUID id,ContaRequest req) {return new DadosConta(id,req.username(),req.nome(),req.email(),req.perfil(),req.ativo(),req.almoxarifados());}
 @PostMapping("/provisionamentos") @ResponseStatus(HttpStatus.ACCEPTED)
 public Response criar(@RequestHeader("Idempotency-Key") UUID id,@Valid @RequestBody ContaRequest req) {return resposta(casoDeUso.solicitar(id,dados(null,req)));}
 @PutMapping("/{keycloakId}/identidade") @ResponseStatus(HttpStatus.ACCEPTED)
 public Response atualizar(@PathVariable UUID keycloakId,@RequestHeader("Idempotency-Key") UUID id,@Valid @RequestBody ContaRequest req) {return resposta(casoDeUso.solicitar(id,dados(keycloakId,req)));}
 @GetMapping("/provisionamentos/{id}") public Response buscar(@PathVariable UUID id) {return resposta(casoDeUso.buscar(id));}
 @PostMapping("/provisionamentos/{id}/reprocessar") @ResponseStatus(HttpStatus.ACCEPTED) public Response reprocessar(@PathVariable UUID id) {return resposta(casoDeUso.reprocessar(id));}
}
