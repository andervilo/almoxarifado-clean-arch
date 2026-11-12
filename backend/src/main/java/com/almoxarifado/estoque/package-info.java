/**
 * Estoque (core domain). Novas dependências permitidas devem ser adicionadas
 * aqui conforme os módulos forem criados, sempre via interface nomeada
 * {@code <modulo>::events} (ex.: "catalogo::events", "estrutura::events").
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Estoque",
    allowedDependencies = {"shared"}
)
package com.almoxarifado.estoque;
