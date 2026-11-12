package com.almoxarifado.estrutura.infrastructure.web;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.estrutura.application.EstruturaService;
import com.almoxarifado.estrutura.domain.*;
@RestController
@RequestMapping("/api/v1/almoxarifados")
public class EstruturaController {
 private final EstruturaService service;
 private final EstruturaMapper mapper;
 public EstruturaController(EstruturaService service,EstruturaMapper mapper) { this.service=service; this.mapper=mapper; }
 public record AlmoxarifadoRequest(@NotBlank @Size(max=120) String codigo,@NotBlank @Size(max=255) String nome,boolean ativo) {}
 public record LocalRequest(@NotBlank @Size(max=120) String codigo,@NotBlank @Size(max=255) String descricao,boolean ativo) {}
 public record AlmoxarifadoResponse(UUID id,String codigo,String nome,boolean ativo) {}
 public record LocalResponse(UUID id,UUID almoxarifadoId,String codigo,String descricao,boolean ativo) {}
 private AlmoxarifadoResponse resposta(Almoxarifado v) { return mapper.resposta(v); }
 private LocalResponse resposta(LocalArmazenamento v) { return mapper.resposta(v); }
 @GetMapping public List<AlmoxarifadoResponse> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.almoxarifados(pagina,tamanho).stream().map(this::resposta).toList(); }
 @GetMapping("/{id}") public AlmoxarifadoResponse buscar(@PathVariable UUID id) { return resposta(service.buscarAlmoxarifado(id)); }
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public AlmoxarifadoResponse criar(@Valid @RequestBody AlmoxarifadoRequest req) { return resposta(service.salvarAlmoxarifado(UUID.randomUUID(),false,req.codigo(),req.nome(),req.ativo())); }
 @PutMapping("/{id}") public AlmoxarifadoResponse atualizar(@PathVariable UUID id,@Valid @RequestBody AlmoxarifadoRequest req) { return resposta(service.salvarAlmoxarifado(id,true,req.codigo(),req.nome(),req.ativo())); }
 @GetMapping("/{almox}/locais") public List<LocalResponse> locais(@PathVariable UUID almox,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.locais(almox,pagina,tamanho).stream().map(this::resposta).toList(); }
 @PostMapping("/{almox}/locais") @ResponseStatus(HttpStatus.CREATED)
 public LocalResponse criarLocal(@PathVariable UUID almox,@Valid @RequestBody LocalRequest req) { return resposta(service.salvarLocal(UUID.randomUUID(),almox,false,req.codigo(),req.descricao(),req.ativo())); }
 @PutMapping("/{almox}/locais/{id}") public LocalResponse atualizarLocal(@PathVariable UUID almox,@PathVariable UUID id,@Valid @RequestBody LocalRequest req) { return resposta(service.salvarLocal(id,almox,true,req.codigo(),req.descricao(),req.ativo())); }
}
