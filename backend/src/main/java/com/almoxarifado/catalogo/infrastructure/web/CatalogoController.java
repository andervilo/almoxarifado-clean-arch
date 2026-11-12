package com.almoxarifado.catalogo.infrastructure.web;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.catalogo.application.CatalogoService;
import com.almoxarifado.catalogo.domain.*;
@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {
 private final CatalogoService service;
 private final CatalogoMapper mapper;
 public CatalogoController(CatalogoService service,CatalogoMapper mapper) { this.service=service; this.mapper=mapper; }
 public record CadastroRequest(@NotBlank @Size(max=120) String codigo,@NotBlank @Size(max=255) String nome,boolean ativo) {}
 public record CadastroResponse(UUID id,String codigo,String nome,boolean ativo) {}
 public record ItemRequest(@NotBlank @Size(max=120) String codigo,@NotBlank @Size(max=255) String descricao,
  @NotNull UUID categoriaId,@NotNull UUID unidadeMedidaId,@NotNull TipoRastreabilidade rastreabilidade,
  @NotNull @Digits(integer=15,fraction=4) BigDecimal estoqueMinimo,@NotNull @Digits(integer=15,fraction=4) BigDecimal estoqueMaximo,boolean ativo) {}
 public record ItemResponse(UUID id,String codigo,String descricao,UUID categoriaId,UUID unidadeMedidaId,
  TipoRastreabilidade rastreabilidade,BigDecimal estoqueMinimo,BigDecimal estoqueMaximo,boolean ativo) {}

 private CadastroResponse resposta(Categoria v) { return mapper.resposta(v); }
 @GetMapping("/categorias")
 public List<CadastroResponse> listarCategoria(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.listarCategoria(pagina,tamanho).stream().map(this::resposta).toList(); }
 @GetMapping("/categorias/{id}")
 public CadastroResponse buscarCategoria(@PathVariable UUID id) { return resposta(service.buscarCategoria(id)); }
 @PostMapping("/categorias") @ResponseStatus(HttpStatus.CREATED)
 public CadastroResponse criarCategoria(@Valid @RequestBody CadastroRequest req) {
  UUID id=UUID.randomUUID(); return resposta(service.salvarCategoria(id,false,new Categoria(id,service.organizacaoId(),req.codigo(),req.nome(),req.ativo())));
 }
 @PutMapping("/categorias/{id}")
 public CadastroResponse atualizarCategoria(@PathVariable UUID id,@Valid @RequestBody CadastroRequest req) {
  return resposta(service.salvarCategoria(id,true,new Categoria(id,service.organizacaoId(),req.codigo(),req.nome(),req.ativo())));
 }

 private CadastroResponse resposta(UnidadeMedida v) { return mapper.resposta(v); }
 @GetMapping("/unidades-medida")
 public List<CadastroResponse> listarUnidadeMedida(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.listarUnidadeMedida(pagina,tamanho).stream().map(this::resposta).toList(); }
 @GetMapping("/unidades-medida/{id}")
 public CadastroResponse buscarUnidadeMedida(@PathVariable UUID id) { return resposta(service.buscarUnidadeMedida(id)); }
 @PostMapping("/unidades-medida") @ResponseStatus(HttpStatus.CREATED)
 public CadastroResponse criarUnidadeMedida(@Valid @RequestBody CadastroRequest req) {
  UUID id=UUID.randomUUID(); return resposta(service.salvarUnidadeMedida(id,false,new UnidadeMedida(id,service.organizacaoId(),req.codigo(),req.nome(),req.ativo())));
 }
 @PutMapping("/unidades-medida/{id}")
 public CadastroResponse atualizarUnidadeMedida(@PathVariable UUID id,@Valid @RequestBody CadastroRequest req) {
  return resposta(service.salvarUnidadeMedida(id,true,new UnidadeMedida(id,service.organizacaoId(),req.codigo(),req.nome(),req.ativo())));
 }

 private CadastroResponse resposta(Fornecedor v) { return mapper.resposta(v); }
 @GetMapping("/fornecedores")
 public List<CadastroResponse> listarFornecedor(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.listarFornecedor(pagina,tamanho).stream().map(this::resposta).toList(); }
 @GetMapping("/fornecedores/{id}")
 public CadastroResponse buscarFornecedor(@PathVariable UUID id) { return resposta(service.buscarFornecedor(id)); }
 @PostMapping("/fornecedores") @ResponseStatus(HttpStatus.CREATED)
 public CadastroResponse criarFornecedor(@Valid @RequestBody CadastroRequest req) {
  UUID id=UUID.randomUUID(); return resposta(service.salvarFornecedor(id,false,new Fornecedor(id,service.organizacaoId(),req.codigo(),req.nome(),req.ativo())));
 }
 @PutMapping("/fornecedores/{id}")
 public CadastroResponse atualizarFornecedor(@PathVariable UUID id,@Valid @RequestBody CadastroRequest req) {
  return resposta(service.salvarFornecedor(id,true,new Fornecedor(id,service.organizacaoId(),req.codigo(),req.nome(),req.ativo())));
 }

 private ItemResponse resposta(Item v) { return mapper.resposta(v); }
 @GetMapping("/itens")
 public List<ItemResponse> listarItem(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) { return service.listarItem(pagina,tamanho).stream().map(this::resposta).toList(); }
 @GetMapping("/itens/{id}")
 public ItemResponse buscarItem(@PathVariable UUID id) { return resposta(service.buscarItem(id)); }
 @PostMapping("/itens") @ResponseStatus(HttpStatus.CREATED)
 public ItemResponse criarItem(@Valid @RequestBody ItemRequest req) {
  UUID id=UUID.randomUUID(); return resposta(service.salvarItem(id,false,new Item(id,service.organizacaoId(),req.codigo(),req.descricao(),req.categoriaId(),req.unidadeMedidaId(),req.rastreabilidade(),req.estoqueMinimo(),req.estoqueMaximo(),req.ativo())));
 }
 @PutMapping("/itens/{id}")
 public ItemResponse atualizarItem(@PathVariable UUID id,@Valid @RequestBody ItemRequest req) {
  return resposta(service.salvarItem(id,true,new Item(id,service.organizacaoId(),req.codigo(),req.descricao(),req.categoriaId(),req.unidadeMedidaId(),req.rastreabilidade(),req.estoqueMinimo(),req.estoqueMaximo(),req.ativo())));
 }

 public record ParametrosRequest(@NotNull @Digits(integer=15,fraction=4) BigDecimal minimo,@NotNull @Digits(integer=15,fraction=4) BigDecimal maximo) {}
 public record ParametrosResponse(UUID itemId,UUID almoxarifadoId,BigDecimal minimo,BigDecimal maximo) {}
 @GetMapping("/itens/{item}/parametros-reposicao/{almox}")
 public ParametrosResponse parametros(@PathVariable UUID item,@PathVariable UUID almox) {
  var p=service.parametros(item,almox);return new ParametrosResponse(item,almox,p.minimo(),p.maximo());
 }
 @PutMapping("/itens/{item}/parametros-reposicao/{almox}")
 public ParametrosResponse configurar(@PathVariable UUID item,@PathVariable UUID almox,@Valid @RequestBody ParametrosRequest req) {
  var p=service.configurarReposicao(item,almox,req.minimo(),req.maximo());return new ParametrosResponse(item,almox,p.minimo(),p.maximo());
 }
}
