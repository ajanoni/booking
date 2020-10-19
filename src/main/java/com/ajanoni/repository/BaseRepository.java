package com.ajanoni.repository;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;

class BaseRepository {

    static final String QUERY_UUID = "SELECT UUID() AS id;";
    private static final String MESSAGE_DB_RESULT_ERROR = "Error on db operation.";

    <T> Uni<T> validateDbReturn(io.vertx.mutiny.sqlclient.RowSet<Row> it, T retValue) {
        if (it.rowCount() > 0) {
            return Uni.createFrom().item(retValue);
        }
        throw new IllegalStateException(MESSAGE_DB_RESULT_ERROR);
    }
}
