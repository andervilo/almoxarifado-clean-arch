package com.almoxarifado.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.almoxarifado.TestcontainersConfiguration;

@ApplicationModuleTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, RegistroEventosIntegrationTest.Configuracao.class})
class RegistroEventosIntegrationTest {

    @Autowired
    ApplicationEventPublisher eventos;
    @Autowired
    PlatformTransactionManager transacoes;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    Consumidor consumidor;

    @Test
    void registraEntregaEConclusaoDoEventoAposCommit() {
        var evento = new EventoFundacao(UUID.randomUUID());
        var threadPublicadora = Thread.currentThread().getName();
        new TransactionTemplate(transacoes).executeWithoutResult(status -> eventos.publishEvent(evento));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(consumidor.recebeu(evento.eventId())).isTrue();
            assertThat(consumidor.threadConsumidora(evento.eventId())).isNotEqualTo(threadPublicadora);
            assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM modulith.event_publication
                WHERE serialized_event::jsonb ->> 'eventId' = ? AND completion_date IS NOT NULL
                """, Integer.class, evento.eventId().toString())).isEqualTo(1);
        });
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).isEqualTo("public");
    }

    @Test
    void rollbackNaoPublicaNemPersisteEvento() {
        var evento = new EventoFundacao(UUID.randomUUID());
        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            eventos.publishEvent(evento);
            status.setRollbackOnly();
        });

        assertThat(consumidor.recebeu(evento.eventId())).isFalse();
        assertThat(jdbc.queryForObject("""
            SELECT count(*) FROM modulith.event_publication
            WHERE serialized_event::jsonb ->> 'eventId' = ?
            """, Integer.class, evento.eventId().toString())).isZero();
    }

    public record EventoFundacao(UUID eventId) {
    }

    public static class Consumidor {
        private final Map<UUID, String> recebidos = new ConcurrentHashMap<>();

        public boolean recebeu(UUID eventId) {
            return recebidos.containsKey(eventId);
        }

        public String threadConsumidora(UUID eventId) {
            return recebidos.get(eventId);
        }

        @ApplicationModuleListener
        public void receber(EventoFundacao evento) {
            recebidos.put(evento.eventId(), Thread.currentThread().getName());
        }
    }

    @TestConfiguration
    static class Configuracao {
        @Bean
        Consumidor consumidorFundacao() {
            return new Consumidor();
        }
    }
}
