package com.ajanoni.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseRepositoryTest {

    private static final String RETURNED_OBJECT = "retObject";

    @Mock
    private RowSet<Row> rowSet;

    private BaseRepository testInstance;

    @BeforeEach
    void setup() {
        testInstance = new BaseRepository();
    }

    @Test
    void returnResult() {
        given(rowSet.rowCount()).willReturn(1);
        String resultValue = testInstance.validateDbReturn(rowSet, RETURNED_OBJECT).await().indefinitely();

        assertThat(resultValue).isEqualTo(RETURNED_OBJECT);
    }

    @Test
    void returnException() {
        given(rowSet.rowCount()).willReturn(0);
        assertThatThrownBy(() -> testInstance.validateDbReturn(rowSet, RETURNED_OBJECT).await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error on db operation.");
    }
}
