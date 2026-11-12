package com.almoxarifado.alerta.infrastructure.web;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import com.almoxarifado.alerta.application.AlertaService;
@RestController @RequestMapping("/api/v1")
public class AlertaController {
 private final AlertaService service;
 public AlertaController(AlertaService service) { this.service=service; }
 public record DispensaRequest(@NotBlank @Size(max=150) String chave,@NotBlank @Size(max=100) String revisao) {}
 public record AlertaResponse(String chave,String tipo,UUID itemId,UUID posicaoId,BigDecimal quantidade,BigDecimal limite,LocalDate validade,String revisao) {}
 @GetMapping("/alertas") public List<AlertaResponse> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) {
  return service.listar(pagina,tamanho).stream().map(a->new AlertaResponse(a.chave(),a.tipo(),a.itemId(),a.posicaoId(),a.quantidade(),a.limite(),a.validade(),a.revisao())).toList();
 }
 @PostMapping("/alertas/dispensar") public void dispensar(@Valid @RequestBody DispensaRequest req) { service.dispensar(req.chave(),req.revisao()); }
 @GetMapping("/dashboard") public Map<String,Object> dashboard() { return service.dashboard(); }
}
