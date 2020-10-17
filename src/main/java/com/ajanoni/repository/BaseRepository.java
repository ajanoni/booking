package com.ajanoni.repository;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;

public class BaseRepository {

    static final String QUERY_UUID = "SELECT UUID() AS id;";

    <T> Uni<T> validateDbReturn(io.vertx.mutiny.sqlclient.RowSet<Row> it, T retValue) {
        if (it.rowCount() > 0) {
            return Uni.createFrom().item(retValue);
        }
        throw new IllegalStateException("Error on db operation.");
    }
}
