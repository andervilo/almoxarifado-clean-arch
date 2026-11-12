package com.almoxarifado.shared.infrastructure.persistence;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
/** Somente UUIDs validados; o array e enviado como parametro JDBC, nunca interpolado no SQL. */
public final class FiltroEscopoSql {
    private FiltroEscopoSql() {}
    public static String array(Set<UUID> ids) {
        return ids==null?null:ids.stream().map(UUID::toString).sorted().collect(Collectors.joining(",","{","}"));
    }
}
