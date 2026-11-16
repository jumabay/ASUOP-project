/**
 * Future JDBC adapters for the standalone {@code RAILDB} database (schema {@code RAIL_APP}).
 *
 * <p>P1: package reserved only. No Spring beans, no DataSource, no network I/O.
 * Activate under profile {@code raildb-test} only after APPROVALS allow JDBC to RAILDB.
 * Do not use the legacy {@code RAIL_APP} schema inside RAC as the production store (ADR-0009).
 */
package platform.oracle.raildb;
