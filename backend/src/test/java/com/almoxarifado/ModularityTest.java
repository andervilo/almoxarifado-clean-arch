package com.almoxarifado;

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
    void geraDocumentacaoDosModulos() {
        new Documenter(MODULES, Documenter.Options.defaults().withOutputFolder("target/modulith-docs"))
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml()
            .writeModuleCanvases();
    }
}
