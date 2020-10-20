package com.ajanoni.repository;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;

class BaseRepository {

    static final String QUERY_UUID = "SELECT UUID() AS id;";
    private static final String MESSAGE_DB_RESULT_ERROR = "Error on db operation.";

    <T> Uni<T> validateDbReturn(RowSet<Row> rows, T retValue) {
        if (rows.rowCount() > 0) {
            return Uni.createFrom().item(retValue);
        }
        throw new IllegalStateException(MESSAGE_DB_RESULT_ERROR);
    }
}
