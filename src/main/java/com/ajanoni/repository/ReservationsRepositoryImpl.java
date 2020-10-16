package com.ajanoni.repository;

import com.ajanoni.model.Reservation;
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
public class ReservationsRepositoryImpl implements ReservationRepository {

    private static final String INSERT_RESERVATION = "INSERT INTO reservations "
            + "(id, customer_id, arrival_date, departure_date) VALUES (?, ?, ?, ?);";

    private static final String UPDATE_RESERVATION = "UPDATE reservations "
            + "SET customer_id = ?, arrival_date = ?, departure_date = ? WHERE id = ?;";

    private static final String DELETE_RESERVATION = "DELETE reservations WHERE id = ?;";

    private static final String QUERY_GET_BY_ID = "SELECT id, customer_id, arrival_date, departure_date "
            + "FROM reservations WHERE id = ?;";

    private static final String QUERY_RESERVED_DATES = "SELECT arrival_date, departure_date "
            + "FROM reservations "
            + "WHERE (arrival_date >= ? AND arrival_date <= ?) "
            + "OR (departure_date >= ? AND departure_date <= ?) ;";

    private static final String QUERY_HAS_RESERVATION = "SELECT 1 FROM reservations "
            + "WHERE (arrival_date >= ? AND arrival_date <= ?) "
            + "OR (departure_date >= ? AND departure_date <= ?) "
            + "LIMIT 1;";

    private static final String QUERY_UUID = "SELECT UUID() AS id;";

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
                    Tuple queryParams = Tuple.of(id, reservation.getCustomerId(), reservation.getArrivalDate(),
                            reservation.getDepartureDate());

                    return connection.preparedQuery(INSERT_RESERVATION)
                            .execute(queryParams)
                            .onItem()
                            .ignore()
                            .andSwitchTo(Uni.createFrom().item(id));
                })
        );
    }

    @Override
    public Uni<Boolean> delete(String id) {
        return SqlClientHelper.usingConnectionUni(client, connection ->
        {
            Tuple queryParams = Tuple.of(id);

            return connection.preparedQuery(DELETE_RESERVATION).execute(queryParams)
                    .onItem()
                    .ignore()
                    .andSwitchTo(Uni.createFrom().item(true));
        });
    }

    @Override
    public Uni<Reservation> update(Reservation reservation) {
        return SqlClientHelper.usingConnectionUni(client, connection ->
        {
            Tuple queryParams = Tuple.of(reservation.getCustomerId(), reservation.getArrivalDate(),
                    reservation.getDepartureDate(), reservation.getId());

            return connection.preparedQuery(UPDATE_RESERVATION)
                    .execute(queryParams)
                    .onItem()
                    .ignore()
                    .andSwitchTo(Uni.createFrom().item(reservation));
        });
    }

    @Override
    public Uni<Reservation> getById(String id) {
        return SqlClientHelper.usingConnectionUni(client, conn ->
                {
                    Tuple queryParams = Tuple.of(id);

                    return conn.preparedQuery(QUERY_GET_BY_ID)
                            .execute(queryParams)
                            .onItem()
                            .transformToUni(this::getReservationUni);
                }
        );
    }

    @Override
    public Uni<Boolean> hasReservationBetween(LocalDate startDate, LocalDate endDate) {
        return SqlClientHelper.usingConnectionUni(client, conn ->
        {
            Tuple queryParams = Tuple.of(startDate, endDate, startDate, endDate);

            return conn.preparedQuery(QUERY_HAS_RESERVATION)
                    .execute(queryParams)
                    .onItem()
                    .transformToUni(rows -> {
                        if (rows.iterator().hasNext()) {
                            return Uni.createFrom().item(true);
                        }

                        return Uni.createFrom().nothing();

                    });
        });
    }

    @Override
    public Multi<LocalDate> getReservedDates(LocalDate startDate, LocalDate endDate) {
        return SqlClientHelper.usingConnectionMulti(client, conn ->
                {
                    Tuple queryParams = Tuple.of(startDate, endDate, startDate, endDate);

                    return conn.preparedQuery(QUERY_RESERVED_DATES)
                            .execute(queryParams)
                            .onItem()
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
            Reservation reservation = new Reservation(row.getString(COLUMN_ID), row.getString(COLUMN_CUSTOMER_ID),
                    row.getLocalDate(COLUMN_ARRIVAL_DATE), row.getLocalDate(COLUMN_DEPARTURE_DATE));
            return Uni.createFrom().item(reservation);
        }

        return Uni.createFrom().nothing();
    }
}
