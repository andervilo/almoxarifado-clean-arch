package com.almoxarifado.estoque.application;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ComandoEstoque(String tipo,UUID itemId,UUID origemId,UUID destinoId,BigDecimal quantidade,
 String lote,String serie,LocalDate validade,BigDecimal custo,String motivo,UUID referenciaId) {}
