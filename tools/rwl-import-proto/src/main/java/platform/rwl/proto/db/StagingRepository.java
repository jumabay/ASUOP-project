package platform.rwl.proto.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import platform.rwl.proto.model.ImportMode;
import platform.rwl.proto.model.RwlOperation;
import platform.rwl.proto.model.RunStatus;
import platform.rwl.proto.parse.ParseOutcome;

public final class StagingRepository {

    private static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private final Connection connection;
    private final DbSupport.Dialect dialect;

    public StagingRepository(Connection connection, DbSupport.Dialect dialect) {
        this.connection = connection;
        this.dialect = dialect;
    }

    public long nextRunId() throws SQLException {
        return nextSeq("rwl_import_run_seq");
    }

    public long insertRunStart(long runId, ImportMode mode, String requestSummary, Instant startedAt)
            throws SQLException {
        String sql =
                """
                INSERT INTO rwl_import_run (
                  run_id, started_at_utc, mode_code, status_code, request_summary,
                  wagons_seen, ops_seen, inserted_cnt, updated_cnt, unchanged_cnt, rejected_cnt)
                VALUES (?, ?, ?, ?, ?, 0, 0, 0, 0, 0, 0)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, runId);
            setUtcTimestamp(ps, 2, startedAt);
            ps.setString(3, mode.name());
            ps.setString(4, "STARTED");
            ps.setString(5, truncate(requestSummary, 1000));
            ps.executeUpdate();
        }
        return runId;
    }

    public void finishRun(
            long runId,
            RunStatus status,
            String errorSummary,
            int wagonsSeen,
            int opsSeen,
            ImportCounters counters,
            Instant finishedAt)
            throws SQLException {
        String sql =
                """
                UPDATE rwl_import_run SET
                  finished_at_utc = ?,
                  status_code = ?,
                  error_summary = ?,
                  wagons_seen = ?,
                  ops_seen = ?,
                  inserted_cnt = ?,
                  updated_cnt = ?,
                  unchanged_cnt = ?,
                  rejected_cnt = ?
                WHERE run_id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            setUtcTimestamp(ps, 1, finishedAt);
            ps.setString(2, status.name());
            if (errorSummary == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, truncate(errorSummary, 2000));
            }
            ps.setInt(4, wagonsSeen);
            ps.setInt(5, opsSeen);
            ps.setInt(6, counters.inserted());
            ps.setInt(7, counters.updated());
            ps.setInt(8, counters.unchanged());
            ps.setInt(9, counters.rejected());
            ps.setLong(10, runId);
            ps.executeUpdate();
        }
    }

    public ImportCounters importOperations(long runId, List<RwlOperation> operations, List<ParseOutcome.RejectedItem> rejects)
            throws SQLException {
        ImportCounters counters = new ImportCounters();
        for (RwlOperation op : operations) {
            applyOperation(runId, op, counters);
        }
        for (ParseOutcome.RejectedItem reject : rejects) {
            insertReject(runId, reject, Instant.now());
            counters.incrementRejected();
        }
        return counters;
    }

    private void applyOperation(long runId, RwlOperation op, ImportCounters counters) throws SQLException {
        String eventKey = op.eventKey().orElseThrow();
        String hash = op.contentHash();
        Optional<CurrentRow> current = findCurrent(eventKey);
        if (current.isEmpty()) {
            long opId = insertOperation(runId, op, eventKey, hash, 1);
            upsertCurrent(eventKey, opId, hash, op.fetchedAtUtc());
            counters.incrementInserted();
            return;
        }
        CurrentRow row = current.get();
        if (hash.equals(row.contentHash())) {
            counters.incrementUnchanged();
            return;
        }
        int nextRev = maxRevision(eventKey) + 1;
        long opId = insertOperation(runId, op, eventKey, hash, nextRev);
        upsertCurrent(eventKey, opId, hash, op.fetchedAtUtc());
        counters.incrementUpdated();
    }

    private Optional<CurrentRow> findCurrent(String eventKey) throws SQLException {
        String sql = "SELECT operation_id, content_hash FROM rwl_staging_current WHERE event_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, eventKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new CurrentRow(rs.getLong(1), rs.getString(2)));
            }
        }
    }

    private int maxRevision(String eventKey) throws SQLException {
        String sql = "SELECT COALESCE(MAX(revision_no), 0) FROM rwl_staging_operation WHERE event_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, eventKey);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private long insertOperation(long runId, RwlOperation op, String eventKey, String hash, int revision)
            throws SQLException {
        long id = nextSeq("rwl_staging_operation_seq");
        String sql =
                """
                INSERT INTO rwl_staging_operation (
                  operation_id, run_id, event_key, content_hash, revision_no,
                  wagon_no, station_code, station_name, operation_time_utc,
                  operation_code, operation_asoup_code, operation_name,
                  train_index, train_number,
                  from_station_code, from_station_name, to_station_code, to_station_name,
                  loaded_flag, cargo_name, etsng_code, weight_tons,
                  latitude, longitude, source_code, fetched_at_utc, created_at_utc)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        Instant now = Instant.now();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, id);
            ps.setLong(i++, runId);
            ps.setString(i++, eventKey);
            ps.setString(i++, hash);
            ps.setInt(i++, revision);
            ps.setString(i++, op.wagonNo());
            ps.setString(i++, emptyToNull(op.stationCode()));
            ps.setString(i++, emptyToNull(op.stationName()));
            setUtcTimestamp(ps, i++, op.operationInstantUtc());
            ps.setString(i++, emptyToNull(op.operationCode()));
            ps.setString(i++, emptyToNull(op.operationAsoupCode()));
            ps.setString(i++, emptyToNull(op.operationName()));
            ps.setString(i++, emptyToNull(op.trainIndex()));
            ps.setString(i++, emptyToNull(op.trainNumber()));
            ps.setString(i++, emptyToNull(op.fromStationCode()));
            ps.setString(i++, emptyToNull(op.fromStationName()));
            ps.setString(i++, emptyToNull(op.toStationCode()));
            ps.setString(i++, emptyToNull(op.toStationName()));
            ps.setString(i++, emptyToNull(op.loadedFlag()));
            ps.setString(i++, emptyToNull(op.cargoName()));
            ps.setString(i++, emptyToNull(op.etsngCode()));
            ps.setString(i++, emptyToNull(op.weightTons()));
            ps.setString(i++, emptyToNull(op.latitude()));
            ps.setString(i++, emptyToNull(op.longitude()));
            ps.setString(i++, "RWL");
            setUtcTimestamp(ps, i++, op.fetchedAtUtc());
            setUtcTimestamp(ps, i, now);
            ps.executeUpdate();
        }
        return id;
    }

    private void upsertCurrent(String eventKey, long operationId, String hash, Instant at) throws SQLException {
        String updateSql =
                """
                UPDATE rwl_staging_current
                SET operation_id = ?, content_hash = ?, updated_at_utc = ?
                WHERE event_key = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setLong(1, operationId);
            ps.setString(2, hash);
            setUtcTimestamp(ps, 3, at);
            ps.setString(4, eventKey);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                return;
            }
        }
        String insertSql =
                """
                INSERT INTO rwl_staging_current (event_key, operation_id, content_hash, updated_at_utc)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setString(1, eventKey);
            ps.setLong(2, operationId);
            ps.setString(3, hash);
            setUtcTimestamp(ps, 4, at);
            try {
                ps.executeUpdate();
            } catch (SQLException e) {
                // Concurrent insert of same event_key: retry as update.
                if (isUniqueViolation(e)) {
                    try (PreparedStatement retry = connection.prepareStatement(updateSql)) {
                        retry.setLong(1, operationId);
                        retry.setString(2, hash);
                        setUtcTimestamp(retry, 3, at);
                        retry.setString(4, eventKey);
                        if (retry.executeUpdate() == 0) {
                            throw e;
                        }
                        return;
                    }
                }
                throw e;
            }
        }
    }

    private static boolean isUniqueViolation(SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return msg.contains("unique") || msg.contains("ora-00001") || "23505".equals(e.getSQLState());
    }

    private void insertReject(long runId, ParseOutcome.RejectedItem reject, Instant at) throws SQLException {
        long id = nextSeq("rwl_staging_reject_seq");
        String sql =
                """
                INSERT INTO rwl_staging_reject (
                  reject_id, run_id, wagon_no, reason_code, detail_text, raw_excerpt,
                  fetched_at_utc, created_at_utc)
                VALUES (?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, runId);
            ps.setString(3, emptyToNull(reject.wagonNo()));
            ps.setString(4, reject.reasonCode());
            ps.setString(5, truncate(reject.detail(), 2000));
            ps.setString(6, truncate(reject.rawExcerpt(), 2000));
            setUtcTimestamp(ps, 7, at);
            setUtcTimestamp(ps, 8, at);
            ps.executeUpdate();
        }
    }

    public List<StoredOperation> readCurrentOperations() throws SQLException {
        String sql =
                """
                SELECT o.event_key, o.content_hash, o.revision_no, o.wagon_no, o.station_code,
                       o.operation_time_utc, o.operation_asoup_code, o.train_index, o.weight_tons
                FROM rwl_staging_current c
                JOIN rwl_staging_operation o ON o.operation_id = c.operation_id
                ORDER BY o.wagon_no, o.operation_time_utc
                """;
        List<StoredOperation> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("operation_time_utc", UTC);
                LocalDateTime utc = ts == null ? null : LocalDateTime.ofInstant(ts.toInstant(), ZoneOffset.UTC);
                out.add(new StoredOperation(
                        rs.getString("event_key"),
                        rs.getString("content_hash"),
                        rs.getInt("revision_no"),
                        rs.getString("wagon_no"),
                        rs.getString("station_code"),
                        utc,
                        rs.getString("operation_asoup_code"),
                        rs.getString("train_index"),
                        rs.getString("weight_tons")));
            }
        }
        return out;
    }

    public Optional<RunRow> findRun(long runId) throws SQLException {
        String sql =
                """
                SELECT status_code, inserted_cnt, updated_cnt, unchanged_cnt, rejected_cnt, error_summary
                FROM rwl_import_run WHERE run_id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new RunRow(
                        rs.getString(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getInt(5),
                        rs.getString(6)));
            }
        }
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() throws SQLException {
        connection.rollback();
    }

    private long nextSeq(String sequenceName) throws SQLException {
        String sql = dialect == DbSupport.Dialect.ORACLE
                ? "SELECT " + sequenceName + ".NEXTVAL FROM dual"
                : "SELECT NEXT VALUE FOR " + sequenceName;
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static void setUtcTimestamp(PreparedStatement ps, int index, Instant instant) throws SQLException {
        if (instant == null) {
            ps.setNull(index, Types.TIMESTAMP);
            return;
        }
        ps.setTimestamp(index, Timestamp.from(instant), UTC);
    }

    private static String emptyToNull(String v) {
        return v == null || v.isEmpty() ? null : v;
    }

    private static String truncate(String v, int max) {
        if (v == null) {
            return null;
        }
        return v.length() <= max ? v : v.substring(0, max);
    }

    private record CurrentRow(long operationId, String contentHash) {
    }

    public record StoredOperation(
            String eventKey,
            String contentHash,
            int revisionNo,
            String wagonNo,
            String stationCode,
            LocalDateTime operationTimeUtc,
            String operationAsoupCode,
            String trainIndex,
            String weightTons) {
    }

    public record RunRow(
            String status,
            int inserted,
            int updated,
            int unchanged,
            int rejected,
            String errorSummary) {
    }

    public static final class ImportCounters {
        private int inserted;
        private int updated;
        private int unchanged;
        private int rejected;

        public void incrementInserted() {
            inserted++;
        }

        public void incrementUpdated() {
            updated++;
        }

        public void incrementUnchanged() {
            unchanged++;
        }

        public void incrementRejected() {
            rejected++;
        }

        public int inserted() {
            return inserted;
        }

        public int updated() {
            return updated;
        }

        public int unchanged() {
            return unchanged;
        }

        public int rejected() {
            return rejected;
        }
    }
}
