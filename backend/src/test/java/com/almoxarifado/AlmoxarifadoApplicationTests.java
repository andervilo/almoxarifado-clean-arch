package com.almoxarifado;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.flywaydb.core.Flyway;
import org.springframework.test.context.ActiveProfiles;

import com.almoxarifado.shared.infrastructure.persistence.FlywayModulosConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AlmoxarifadoApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private FlywayMigrationStrategy migracaoPorModulo;

    @Test
    void contextoSobeComMigracoesAplicadasEmPostgres() {
        List<String> schemas = jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata", String.class);

        assertThat(schemas).containsAll(FlywayModulosConfig.SCHEMAS_MODULOS);
    }

    @Test
    void cadaModuloPossuiHistoricoDeMigracaoProprio() {
        Integer migracoesEstoque = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM estoque.flyway_schema_history WHERE success AND version = '1'", Integer.class);
        Integer migracoesModulith = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM modulith.flyway_schema_history WHERE success AND version = '1'", Integer.class);

        assertThat(migracoesEstoque).isEqualTo(1);
        assertThat(migracoesModulith).isEqualTo(1);
    }

    @Test
    void reiniciarMigracoesPreservaDadosEHistorico() {
        jdbcTemplate.update("INSERT INTO estoque.itens_estoque (codigo, descricao) VALUES (?, ?)",
            "MIGRACAO-REINICIO", "Item preservado");
        try {
            migracaoPorModulo.migrate(flyway);
            assertThat(jdbcTemplate.queryForObject(
                "SELECT descricao FROM estoque.itens_estoque WHERE codigo = 'MIGRACAO-REINICIO'", String.class))
                .isEqualTo("Item preservado");
            cadaModuloPossuiHistoricoDeMigracaoProprio();
        } finally {
            jdbcTemplate.update("DELETE FROM estoque.itens_estoque WHERE codigo = 'MIGRACAO-REINICIO'");
        }
    }

    @Test
    void tabelasLegadasNaoImpedemV1NemSaoAlteradas() {
        assertThat(jdbcTemplate.queryForObject(
            "SELECT name FROM public.realm WHERE id = 'realm-legado'", String.class))
            .isEqualTo("almoxarifado-legado");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT to_regclass('estoque.itens_estoque')::text", String.class))
            .isEqualTo("estoque.itens_estoque");
        cadaModuloPossuiHistoricoDeMigracaoProprio();
    }
}
