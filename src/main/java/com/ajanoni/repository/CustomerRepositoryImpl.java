package com.ajanoni.repository;

import com.ajanoni.repository.model.Customer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.SqlClientHelper;
import io.vertx.mutiny.sqlclient.Tuple;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CustomerRepositoryImpl extends BaseRepository  implements CustomerRepository {

    private static final String INSERT_CUSTOMER = "INSERT INTO customers (id, email, full_name) VALUES (?, ?, ?);";
    private static final String UPDATE_CUSTOMER = "UPDATE customers SET email = ?, full_name = ? WHERE id = ?;";
    private static final String QUERY_GET_BY_EMAIL = "SELECT id, email, full_name FROM customers WHERE email = ?;";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_FULL_NAME = "full_name";

    private final MySQLPool client;

    @Inject
    public CustomerRepositoryImpl(MySQLPool client) {
        this.client = client;
    }

    @Override
    public Uni<String> save(Customer customer) {
        return SqlClientHelper.inTransactionUni(client, tx ->
                tx.preparedQuery(QUERY_UUID).execute().onItem().transformToUni(rowId -> {
                    String id = rowId.iterator().next().getString(COLUMN_ID);
                    Tuple queryParams = Tuple.of(id, customer.email(), customer.fullName());

                    return tx.preparedQuery(INSERT_CUSTOMER)
                            .execute(queryParams).onItem()
                            .transformToUni(it -> validateDbReturn(it, id));
                })
        );
    }

    public Uni<Customer> update(Customer customer) {
        return SqlClientHelper.inTransactionUni(client, tx ->
        {
            Tuple queryParams = Tuple.of(customer.email(), customer.fullName(), customer.id());

            return tx.preparedQuery(UPDATE_CUSTOMER)
                    .execute(queryParams).onItem()
                    .transformToUni(it -> validateDbReturn(it, customer));
        });
    }

    public Uni<Customer> getByEmail(String email) {
        return SqlClientHelper.usingConnectionUni(client, conn ->
                {
                    Tuple queryParams = Tuple.of(email);

                    return conn.preparedQuery(QUERY_GET_BY_EMAIL)
                            .execute(queryParams).onItem()
                            .transformToUni(this::getCustomerUni);
                }
        );
    }

    private Uni<Customer> getCustomerUni(Iterable<Row> rowCustomer) {
        if (rowCustomer.iterator().hasNext()) {
            Row row = rowCustomer
                    .iterator()
                    .next();

            Customer customer = Customer.builder()
                    .id(row.getString(COLUMN_ID))
                    .email(row.getString(COLUMN_EMAIL))
                    .fullName(row.getString(COLUMN_FULL_NAME))
                    .build();

            return Uni.createFrom().item(customer);
        }

        return Uni.createFrom().nullItem();
    }
}
