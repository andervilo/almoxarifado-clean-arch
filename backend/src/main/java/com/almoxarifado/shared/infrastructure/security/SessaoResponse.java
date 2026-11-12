package com.almoxarifado.shared.infrastructure.security;

import java.util.List;
import java.util.UUID;

public record SessaoResponse(String usuarioId, String nomeUsuario, UUID organizacaoId, List<String> perfis) {
}
