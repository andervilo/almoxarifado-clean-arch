package com.almoxarifado.alerta.domain;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
public record Alerta(String chave,String tipo,UUID itemId,UUID posicaoId,BigDecimal quantidade,
 BigDecimal limite,LocalDate validade,String revisao,UUID almoxarifadoId) {}
