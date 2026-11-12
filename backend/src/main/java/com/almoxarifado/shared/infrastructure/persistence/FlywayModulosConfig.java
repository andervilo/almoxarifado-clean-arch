package com.almoxarifado.shared.infrastructure.persistence;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Executa as migrações do Flyway separadamente para cada módulo.
 * <p>
 * Cada módulo possui:
 * <ul>
 *   <li>um schema próprio no PostgreSQL (mesmo nome do módulo);</li>
 *   <li>uma pasta de scripts em {@code db/migration/<modulo>};</li>
 *   <li>seu próprio histórico ({@code <modulo>.flyway_schema_history}), com versionamento independente.</li>
 * </ul>
 * O Spring Boot garante que esta estratégia rode antes da inicialização do JPA.
 */
@Configuration
public class FlywayModulosConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayModulosConfig.class);

    /** Ordem de execução das migrações. {@code modulith} guarda o Event Publication Registry. */
    public static final List<String> SCHEMAS_MODULOS = List.of(
        "modulith",
        "organizacao",
        "catalogo",
        "estrutura",
        "estoque",
        "requisicao",
        "inventario",
        "alerta"
    );

    @Bean
    public FlywayMigrationStrategy migracaoPorModulo() {
        return flywayBase -> SCHEMAS_MODULOS.forEach(schema -> {
            log.info("Aplicando migrações do módulo '{}'", schema);
            Flyway.configure()
                .configuration(flywayBase.getConfiguration())
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .locations("classpath:db/migration/" + schema)
                .failOnMissingLocations(false)
                .load()
                .migrate();
        });
    }
}
