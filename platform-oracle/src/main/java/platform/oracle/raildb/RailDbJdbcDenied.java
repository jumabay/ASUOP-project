package platform.oracle.raildb;

/**
 * Shared fail-fast diagnostic for P4-0 {@code raildb-test} stubs.
 * No DataSource / JDBC until a later APPROVED stage.
 */
public final class RailDbJdbcDenied {

    public static final String MESSAGE =
            "RAILDB JDBC DENIED UNTIL APPROVED — P4-0 stubs only (profile raildb-test; no DataSource)";

    private RailDbJdbcDenied() {
    }

    public static UnsupportedOperationException exception() {
        return new UnsupportedOperationException(MESSAGE);
    }
}
