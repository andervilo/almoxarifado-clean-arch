package com.almoxarifado.organizacao.infrastructure.persistence;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.almoxarifado.organizacao.application.OrganizacaoRepository;
import com.almoxarifado.organizacao.domain.*;
@Repository
public class OrganizacaoJdbc implements OrganizacaoRepository {
    private final JdbcTemplate jdbc;
    public OrganizacaoJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<Organizacao> buscar(UUID id) {
        return jdbc.query("SELECT * FROM organizacao.organizacoes WHERE id = ?", (rs,n) -> {
            var org = new Organizacao(rs.getString("nome"), rs.getString("codigo"));
            if (!rs.getBoolean("ativa")) org.inativar();
            return org;
        }, id).stream().findFirst();
    }
    public void salvar(UUID id, Organizacao org) {
        jdbc.update("""
            INSERT INTO organizacao.organizacoes (id,nome,codigo,ativa) VALUES (?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET nome=excluded.nome,codigo=excluded.codigo,ativa=excluded.ativa
            """, id,org.getNome(),org.getCodigo(),org.isAtiva());
    }
    public List<Setor> setores(UUID org) {
        return jdbc.query("SELECT * FROM organizacao.setores WHERE organizacao_id=? ORDER BY codigo",
            (rs,n)->new Setor(rs.getString("codigo"),rs.getString("nome")), org);
    }
    public void salvarSetor(UUID org, Setor setor) {
        jdbc.update("""
            INSERT INTO organizacao.setores (organizacao_id,codigo,nome) VALUES (?,?,?)
            ON CONFLICT (organizacao_id,codigo) DO UPDATE SET nome=excluded.nome
            """, org,setor.getCodigo(),setor.getNome());
    }
    public List<Usuario> usuarios(UUID org) {
        return jdbc.query("SELECT * FROM organizacao.usuarios WHERE organizacao_id=? ORDER BY codigo", (rs,n)->{
            var usuario = new Usuario(rs.getString("codigo"),rs.getString("nome"),rs.getString("email"),
                PerfilUsuario.valueOf(rs.getString("perfil")),org.toString());
            if (!rs.getBoolean("ativo")) usuario.inativar();
            return usuario;
        }, org);
    }
    public void salvarUsuario(UUID org, Usuario usuario) {
        jdbc.update("""
            INSERT INTO organizacao.usuarios (organizacao_id,codigo,nome,email,perfil,ativo) VALUES (?,?,?,?,?,?)
            ON CONFLICT (organizacao_id,codigo) DO UPDATE SET nome=excluded.nome,email=excluded.email,
                perfil=excluded.perfil,ativo=excluded.ativo
            """,org,usuario.getCodigo(),usuario.getNome(),usuario.getEmail(),usuario.getPerfil().name(),usuario.isAtivo());
    }
}
