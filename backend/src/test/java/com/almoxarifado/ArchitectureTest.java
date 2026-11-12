package com.almoxarifado;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regras de Clean Architecture válidas para todos os módulos:
 * domain -> nada de framework; application -> não conhece infraestrutura.
 */
@AnalyzeClasses(packages = "com.almoxarifado", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dominioNaoDependeDeFramework = noClasses()
        .that().resideInAPackage("com.almoxarifado..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
            "com.fasterxml.jackson.."
        )
        .because("o domínio deve ser independente de framework (AGENTS.md)");

    @ArchTest
    static final ArchRule dominioNaoDependeDeOutrasCamadas = noClasses()
        .that().resideInAPackage("com.almoxarifado..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "com.almoxarifado..application..",
            "com.almoxarifado..infrastructure.."
        );

    @ArchTest
    static final ArchRule aplicacaoNaoDependeDeInfraestrutura = noClasses()
        .that().resideInAPackage("com.almoxarifado..application..")
        .should().dependOnClassesThat().resideInAPackage("com.almoxarifado..infrastructure..")
        .allowEmptyShould(true);

    @ArchTest
    static final ArchRule aplicacaoNaoConheceWebNemJpa = noClasses()
        .that().resideInAPackage("com.almoxarifado..application..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework.web..",
            "jakarta.persistence..",
            "org.springframework.data.jpa.."
        )
        .allowEmptyShould(true);
}
