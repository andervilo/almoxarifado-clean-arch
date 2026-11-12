package com.almoxarifado.shared.infrastructure.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.almoxarifado.shared.application.Auditoria;

@Repository
public class AuditoriaJdbc implements Auditoria {
    private final JdbcTemplate jdbc;
    public AuditoriaJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void registrar(UUID organizacaoId, String usuarioId, String acao, String recurso) {
        jdbc.update("INSERT INTO shared.auditoria (id, organizacao_id, usuario_id, acao, recurso) VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID(), organizacaoId, usuarioId, acao, recurso);
    }
}
