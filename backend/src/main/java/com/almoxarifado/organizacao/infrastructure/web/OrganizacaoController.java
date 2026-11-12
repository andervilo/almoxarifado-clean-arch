package com.almoxarifado.organizacao.infrastructure.web;
import java.net.URI;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.organizacao.application.OrganizacaoService;
import com.almoxarifado.organizacao.domain.PerfilUsuario;
@RestController
@RequestMapping("/api/v1")
public class OrganizacaoController {
    private final OrganizacaoService service;
    public OrganizacaoController(OrganizacaoService service) { this.service=service; }
    public record CriarOrganizacao(@NotNull UUID id, @NotBlank @Size(max=255) String nome, @NotBlank @Size(max=120) String codigo) {}
    public record AtualizarOrganizacao(@NotBlank @Size(max=255) String nome,@NotBlank @Size(max=120) String codigo,boolean ativa) {}
    public record OrganizacaoResponse(String nome,String codigo,boolean ativa) {}
    public record ConfiguracaoRequest(@Min(1) @Max(365) int diasAlertaValidade) {}
    public record ConfiguracaoResponse(int diasAlertaValidade) {}
    @GetMapping("/organizacao/configuracoes")
    public ConfiguracaoResponse configuracao() { return new ConfiguracaoResponse(service.configuracao().diasAlertaValidade()); }
    @PutMapping("/organizacao/configuracoes")
    public void configurar(@Valid @RequestBody ConfiguracaoRequest req) { service.configurar(req.diasAlertaValidade()); }
    public record SetorRequest(@NotBlank @Size(max=120) String codigo,@NotBlank @Size(max=255) String nome) {}
    public record SetorResponse(String codigo,String nome) {}
    public record UsuarioRequest(@NotBlank @Size(max=255) String codigo,@NotBlank @Size(max=255) String nome,
        @NotBlank @Email @Size(max=255) String email,@NotNull PerfilUsuario perfil,boolean ativo) {}
    public record UsuarioResponse(String codigo,String nome,String email,PerfilUsuario perfil,boolean ativo) {}
    @PostMapping("/organizacoes")
    public ResponseEntity<Map<String,UUID>> criar(@Valid @RequestBody CriarOrganizacao req) {
        var id=service.criar(req.id(),req.nome(),req.codigo());
        return ResponseEntity.created(URI.create("/api/v1/organizacao")).body(Map.of("id",id));
    }
    @GetMapping("/organizacao")
    public OrganizacaoResponse consultar() {
        var org=service.consultar(); return new OrganizacaoResponse(org.getNome(),org.getCodigo(),org.isAtiva());
    }
    @PutMapping("/organizacao")
    public void atualizar(@Valid @RequestBody AtualizarOrganizacao req) { service.atualizar(req.nome(),req.codigo(),req.ativa()); }
    @GetMapping("/setores")
    public List<SetorResponse> setores() { return service.setores().stream().map(s->new SetorResponse(s.getCodigo(),s.getNome())).toList(); }
    @PutMapping("/setores")
    public void setor(@Valid @RequestBody SetorRequest req) { service.salvarSetor(req.codigo(),req.nome()); }
    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios() { return service.usuarios().stream().map(u->new UsuarioResponse(u.getCodigo(),u.getNome(),u.getEmail(),u.getPerfil(),u.isAtivo())).toList(); }
    @PutMapping("/usuarios")
    public void usuario(@Valid @RequestBody UsuarioRequest req) { service.salvarUsuario(req.codigo(),req.nome(),req.email(),req.perfil(),req.ativo()); }
}
