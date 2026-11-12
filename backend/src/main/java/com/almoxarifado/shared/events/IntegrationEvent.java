package com.almoxarifado.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base de todo evento de integração entre módulos.
 * Implementações devem ser {@code record}s imutáveis, nomeadas no passado,
 * contendo apenas tipos simples (UUID, String, BigDecimal, Instant, LocalDate, enums).
 */
public interface IntegrationEvent {

    /** Identificador único do evento, usado para idempotência no consumidor. */
    UUID eventId();

    Instant occurredAt();

    /** Tenant ao qual o evento pertence (obrigatório). */
    UUID organizacaoId();

    /** Versão do contrato do evento. */
    int version();
}
