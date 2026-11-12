package com.almoxarifado;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.almoxarifado.shared.application.PublicadorEventos;
import com.almoxarifado.shared.domain.RegraNegocioException;
import com.almoxarifado.shared.events.IntegrationEvent;
import com.almoxarifado.shared.infrastructure.sql.FiltroEscopoSql;
import com.almoxarifado.shared.infrastructure.persistence.PublicadorEventosSpring;
import com.almoxarifado.shared.infrastructure.security.ContextoUsuarioJwt;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Garante as fronteiras entre módulos do Spring Modulith (sem ciclos, sem acesso
 * a pacotes internos de outros módulos) e gera a documentação dos módulos.
 */
class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(AlmoxarifadoApplication.class);

    @Test
    void verificaEstruturaModular() {
        MODULES.verify();
    }

    @Test
    void sharedExpoeContratosMasEncapsulaImplementacoes() {
        var shared = MODULES.getModuleByName("shared").orElseThrow();
        var interfaces = shared.getNamedInterfaces();
        assertThat(interfaces.getByName("application").orElseThrow().contains(PublicadorEventos.class)).isTrue();
        assertThat(interfaces.getByName("domain").orElseThrow().contains(RegraNegocioException.class)).isTrue();
        assertThat(interfaces.getByName("events").orElseThrow().contains(IntegrationEvent.class)).isTrue();
        assertThat(interfaces.getByName("sql").orElseThrow().contains(FiltroEscopoSql.class)).isTrue();
        var classes = new ClassFileImporter().importClasses(PublicadorEventosSpring.class, ContextoUsuarioJwt.class);
        assertThat(shared.isExposed(classes.get(PublicadorEventosSpring.class))).isFalse();
        assertThat(shared.isExposed(classes.get(ContextoUsuarioJwt.class))).isFalse();
    }

    @Test
    void geraDocumentacaoDosModulos() {
        new Documenter(MODULES, Documenter.Options.defaults().withOutputFolder("target/modulith-docs"))
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml()
            .writeModuleCanvases();
    }
}
