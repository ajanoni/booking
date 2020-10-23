package com.ajanoni.repository;

import com.ajanoni.repository.model.Reservation;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.SqlClientHelper;
import io.vertx.mutiny.sqlclient.Tuple;
import java.time.LocalDate;
import java.util.stream.StreamSupport;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ReservationsRepositoryImpl extends BaseRepository implements ReservationRepository {

    private static final String INSERT_RESERVATION = "INSERT INTO reservations "
            + "(id, customer_id, arrival_date, departure_date) VALUES (?, ?, ?, ?);";

    private static final String UPDATE_RESERVATION = "UPDATE reservations "
            + "SET arrival_date = ?, departure_date = ? WHERE id = ?;";

    private static final String DELETE_RESERVATION = "DELETE FROM reservations WHERE id = ?;";

    private static final String QUERY_GET_BY_ID = "SELECT id, customer_id, arrival_date, departure_date "
            + "FROM reservations WHERE id = ?;";

    private static final String QUERY_RESERVED_DATES = "SELECT arrival_date, departure_date "
            + "FROM reservations "
            + "WHERE (arrival_date >= ? AND arrival_date <= ?) "
            + "OR (departure_date >= ? AND departure_date <= ?) ;";

    private static final String QUERY_HAS_RESERVATION = "SELECT 1 FROM reservations "
            + "WHERE id <> ? "
            + "AND ((arrival_date >= ? AND arrival_date <= ?) "
            + "OR (departure_date >= ? AND departure_date <= ?))"
            + "LIMIT 1;";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CUSTOMER_ID = "customer_id";
    private static final String COLUMN_ARRIVAL_DATE = "arrival_date";
    private static final String COLUMN_DEPARTURE_DATE = "departure_date";

    private final MySQLPool client;

    @Inject
    public ReservationsRepositoryImpl(MySQLPool client) {
        this.client = client;
    }

    @Override
    public Uni<String> save(Reservation reservation) {
        return SqlClientHelper.usingConnectionUni(client, connection ->
                connection.preparedQuery(QUERY_UUID).execute().onItem().transformToUni(rowId -> {
                    String id = rowId.iterator().next().getString(COLUMN_ID);
                    Tuple queryParams = Tuple.of(id, reservation.customerId(),
                            reservation.arrivalDate(),
                            reservation.departureDate());

                    return connection.preparedQuery(INSERT_RESERVATION)
                            .execute(queryParams).onItem()
                            .transformToUni(it -> validateDbReturn(it, id));
                })
        );
    }

    @Override
    public Uni<Boolean> delete(String id) {
        return SqlClientHelper.usingConnectionUni(client, connection ->
        {
            Tuple queryParams = Tuple.of(id);

            return connection.preparedQuery(DELETE_RESERVATION)
                    .execute(queryParams).onItem()
                    .transformToUni(it -> validateDbReturn(it, true));
        });
    }

    @Override
    public Uni<Reservation> update(Reservation reservation) {
        return SqlClientHelper.usingConnectionUni(client, connection ->
        {
            Tuple queryParams = Tuple.of(reservation.arrivalDate(),
                    reservation.departureDate(),
                    reservation.id());

            return connection.preparedQuery(UPDATE_RESERVATION)
                    .execute(queryParams).onItem()
                    .transformToUni(it -> validateDbReturn(it, reservation));
        });
    }

    @Override
    public Uni<Reservation> getById(String id) {
        return SqlClientHelper.usingConnectionUni(client, conn ->
                {
                    Tuple queryParams = Tuple.of(id);

                    return conn.preparedQuery(QUERY_GET_BY_ID)
                            .execute(queryParams).onItem()
                            .transformToUni(this::getReservationUni);
                }
        );
    }

    @Override
    public Uni<Boolean> hasReservationBetween(String reservationId, LocalDate startDate, LocalDate endDate) {
        return SqlClientHelper.usingConnectionUni(client, conn ->
        {
            Tuple queryParams = Tuple.of(reservationId, startDate, endDate, startDate, endDate);

            return conn.preparedQuery(QUERY_HAS_RESERVATION)
                    .execute(queryParams).onItem()
                    .transformToUni(rows ->
                            rows.iterator().hasNext() ? Uni.createFrom().item(true) : Uni.createFrom().item(false));
        });
    }

    @Override
    public Multi<LocalDate> getReservedDates(LocalDate startDate, LocalDate endDate) {
        return SqlClientHelper.usingConnectionMulti(client, conn ->
                {
                    Tuple queryParams = Tuple.of(startDate, endDate, startDate, endDate);

                    return conn.preparedQuery(QUERY_RESERVED_DATES)
                            .execute(queryParams).onItem()
                            .transformToMulti(rows ->
                                    Multi.createFrom()
                                            .emitter(emitter -> emmitSequenceDates(rows, startDate, endDate, emitter)));
                }
        );
    }

    private void emmitSequenceDates(Iterable<Row> rows, LocalDate startDate, LocalDate endDate,
            MultiEmitter<? super LocalDate> emitter) {

        StreamSupport.stream(rows.spliterator(), false).forEach(row -> {
            LocalDate arrivalDate = row.getLocalDateTime(COLUMN_ARRIVAL_DATE).toLocalDate();
            LocalDate departureDateDate = row.getLocalDateTime(COLUMN_DEPARTURE_DATE).toLocalDate();

            LocalDate startSequence = arrivalDate.isBefore(startDate) ? startDate : arrivalDate;
            LocalDate endSequence = departureDateDate.isAfter(endDate) ? endDate : departureDateDate;
            startSequence.datesUntil(endSequence.plusDays(1)).forEach(emitter::emit);
        });

        emitter.complete();
    }

    private Uni<Reservation> getReservationUni(Iterable<Row> rowReservation) {
        if (rowReservation.iterator().hasNext()) {
            Row row = rowReservation.iterator().next();
            Reservation reservation = Reservation.builder()
                    .id(row.getString(COLUMN_ID))
                    .customerId( row.getString(COLUMN_CUSTOMER_ID))
                    .arrivalDate(row.getLocalDateTime(COLUMN_ARRIVAL_DATE).toLocalDate())
                    .departureDate(row.getLocalDateTime(COLUMN_DEPARTURE_DATE).toLocalDate())
                    .build();

            return Uni.createFrom().item(reservation);
        }

        return Uni.createFrom().nullItem();
    }
}
