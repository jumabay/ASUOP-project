package platform.rwl.proto.importing;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import platform.rwl.proto.config.RwlConfig;
import platform.rwl.proto.db.DbSupport;
import platform.rwl.proto.db.StagingRepository;
import platform.rwl.proto.db.StagingRepository.ImportCounters;
import platform.rwl.proto.http.RwlClient;
import platform.rwl.proto.http.UrlMasker;
import platform.rwl.proto.model.ImportMode;
import platform.rwl.proto.model.RwlOperation;
import platform.rwl.proto.model.RunStatus;
import platform.rwl.proto.parse.ParseOutcome;
import platform.rwl.proto.parse.RwlXmlParser;

public final class ImportService {

    private final RwlConfig config;
    private final RwlXmlParser parser = new RwlXmlParser();

    public ImportService(RwlConfig config) {
        this.config = config;
    }

    public ImportReport run(Connection connection, DbSupport.Dialect dialect) throws Exception {
        Instant started = Instant.now();
        FetchParseResult fetched = fetchAndParse();
        ImportReport.Builder report = ImportReport.builder()
                .mode(config.mode())
                .requestSummary(config.requestSummary())
                .parseStatus(fetched.outcome().status())
                .errorSummary(fetched.outcome().errorSummary())
                .wagonsSeen(fetched.outcome().wagonsSeen())
                .operations(fetched.outcome().operations())
                .rejects(fetched.outcome().rejects())
                .rawBodies(fetched.rawBodies());

        if (fetched.outcome().status() == RunStatus.API_ERROR
                || fetched.outcome().status() == RunStatus.PARSE_ERROR) {
            if (config.mode() == ImportMode.IMPORT && connection != null) {
                persistFailedParse(connection, dialect, started, fetched.outcome());
            }
            return report.dbStatus(fetched.outcome().status()).build();
        }

        if (config.mode() == ImportMode.DRY_RUN || connection == null) {
            RunStatus st = fetched.outcome().operations().isEmpty() && fetched.outcome().rejects().isEmpty()
                    ? RunStatus.EMPTY
                    : fetched.outcome().status();
            return report.dbStatus(st)
                    .counters(new ImportCounters())
                    .build();
        }

        StagingRepository repo = new StagingRepository(connection, dialect);
        long runId = repo.nextRunId();
        repo.insertRunStart(runId, ImportMode.IMPORT, config.requestSummary(), started);
        connection.commit();

        try {
            if (config.forceWriteFailure()) {
                throw new SQLException("forced_write_failure");
            }
            ImportCounters counters =
                    repo.importOperations(runId, fetched.outcome().operations(), fetched.outcome().rejects());
            RunStatus status = resolveSuccessStatus(fetched.outcome(), counters);
            repo.finishRun(
                    runId,
                    status,
                    null,
                    fetched.outcome().wagonsSeen(),
                    fetched.outcome().operations().size(),
                    counters,
                    Instant.now());
            connection.commit();
            List<StagingRepository.StoredOperation> stored = repo.readCurrentOperations();
            return report.runId(runId)
                    .dbStatus(status)
                    .counters(counters)
                    .stored(stored)
                    .build();
        } catch (SQLException e) {
            // Roll back operation/reject writes. Run header was committed earlier as STARTED.
            connection.rollback();
            ImportCounters zero = new ImportCounters();
            repo.finishRun(
                    runId,
                    RunStatus.FAILED,
                    UrlMasker.mask(e.getMessage()),
                    fetched.outcome().wagonsSeen(),
                    fetched.outcome().operations().size(),
                    zero,
                    Instant.now());
            connection.commit();
            return report.runId(runId)
                    .dbStatus(RunStatus.FAILED)
                    .errorSummary(UrlMasker.mask(e.getMessage()))
                    .counters(zero)
                    .build();
        }
    }

    private void persistFailedParse(
            Connection connection, DbSupport.Dialect dialect, Instant started, ParseOutcome outcome)
            throws SQLException {
        StagingRepository repo = new StagingRepository(connection, dialect);
        long runId = repo.nextRunId();
        repo.insertRunStart(runId, ImportMode.IMPORT, config.requestSummary(), started);
        repo.finishRun(
                runId,
                outcome.status(),
                outcome.errorSummary(),
                outcome.wagonsSeen(),
                0,
                new ImportCounters(),
                Instant.now());
        connection.commit();
    }

    private FetchParseResult fetchAndParse() throws IOException, InterruptedException {
        Instant fetchedAt = Instant.now();
        List<String> bodies = new ArrayList<>();
        if (config.fixturePath() != null) {
            String body = Files.readString(config.fixturePath());
            bodies.add(body);
            return new FetchParseResult(parser.parse(body, fetchedAt), bodies);
        }

        if (!config.canCallLiveRwl()) {
            return new FetchParseResult(
                    ParseOutcome.parseError("missing_live_settings: " + String.join(", ", config.missingForLiveRwl())),
                    List.of());
        }

        RwlClient client = new RwlClient(config);
        List<RwlClient.FetchedBody> fetched = client.fetchViewVagonBodies();
        ParseOutcome.Builder merged = new ParseOutcome.Builder();
        List<RwlOperation> ops = new ArrayList<>();
        List<ParseOutcome.RejectedItem> rejects = new ArrayList<>();
        int wagons = 0;

        for (RwlClient.FetchedBody item : fetched) {
            bodies.add(item.body());
            if (item.httpStatus() != 200) {
                return new FetchParseResult(
                        ParseOutcome.apiError("http_status_" + item.httpStatus() + " url=" + item.maskedUrl()),
                        bodies);
            }
            ParseOutcome one = parser.parse(item.body(), fetchedAt);
            if (one.status() == RunStatus.API_ERROR || one.status() == RunStatus.PARSE_ERROR) {
                return new FetchParseResult(one, bodies);
            }
            wagons += one.wagonsSeen();
            ops.addAll(one.operations());
            rejects.addAll(one.rejects());
        }

        for (RwlOperation op : ops) {
            merged.addOperation(op);
        }
        // rebuild via success helper
        ParseOutcome outcome = ParseOutcome.success(ops, rejects, wagons);
        return new FetchParseResult(outcome, bodies);
    }

    private static RunStatus resolveSuccessStatus(ParseOutcome outcome, ImportCounters counters) {
        if (outcome.operations().isEmpty() && outcome.rejects().isEmpty()) {
            return RunStatus.EMPTY;
        }
        if (outcome.operations().isEmpty() && counters.rejected() > 0 && counters.inserted() == 0) {
            // only rejects stored
            return RunStatus.SUCCESS;
        }
        return RunStatus.SUCCESS;
    }

    private record FetchParseResult(ParseOutcome outcome, List<String> rawBodies) {
    }

    public static final class ImportReport {
        private final ImportMode mode;
        private final String requestSummary;
        private final RunStatus parseStatus;
        private final RunStatus dbStatus;
        private final String errorSummary;
        private final Long runId;
        private final int wagonsSeen;
        private final List<RwlOperation> operations;
        private final List<ParseOutcome.RejectedItem> rejects;
        private final ImportCounters counters;
        private final List<StagingRepository.StoredOperation> stored;
        private final List<String> rawBodies;

        private ImportReport(Builder b) {
            this.mode = b.mode;
            this.requestSummary = b.requestSummary;
            this.parseStatus = b.parseStatus;
            this.dbStatus = b.dbStatus;
            this.errorSummary = b.errorSummary;
            this.runId = b.runId;
            this.wagonsSeen = b.wagonsSeen;
            this.operations = b.operations == null ? List.of() : List.copyOf(b.operations);
            this.rejects = b.rejects == null ? List.of() : List.copyOf(b.rejects);
            this.counters = b.counters == null ? new ImportCounters() : b.counters;
            this.stored = b.stored == null ? List.of() : List.copyOf(b.stored);
            this.rawBodies = b.rawBodies == null ? List.of() : List.copyOf(b.rawBodies);
        }

        public static Builder builder() {
            return new Builder();
        }

        public void print() {
            System.out.println("=== RWL import proto ===");
            System.out.println("Mode: " + mode);
            System.out.println("Request: " + requestSummary);
            System.out.println("Parse status: " + parseStatus);
            System.out.println("DB status: " + dbStatus);
            if (runId != null) {
                System.out.println("Run id: " + runId);
            }
            System.out.println("Wagons seen: " + wagonsSeen);
            System.out.println("Operations parsed: " + operations.size());
            System.out.println("Rejects: " + rejects.size());
            System.out.println(
                    "Counters inserted/updated/unchanged/rejected: "
                            + counters.inserted()
                            + "/"
                            + counters.updated()
                            + "/"
                            + counters.unchanged()
                            + "/"
                            + counters.rejected());
            if (errorSummary != null) {
                System.out.println("Error: " + UrlMasker.mask(errorSummary));
            }
            if (!operations.isEmpty()) {
                System.out.println("-- Parsed operations (UTC) --");
                for (RwlOperation op : operations) {
                    System.out.println(
                            "  "
                                    + op.wagonNo()
                                    + " | "
                                    + op.operationTimeUtc()
                                    + "Z"
                                    + " | "
                                    + op.stationCode()
                                    + " | "
                                    + op.operationAsoupCode()
                                    + " | key="
                                    + op.eventKey().orElse("?")
                                    + " | hash="
                                    + op.contentHash().substring(0, 12)
                                    + " | fetched="
                                    + op.fetchedAtUtc().atOffset(ZoneOffset.UTC));
                }
            }
            if (!rejects.isEmpty()) {
                System.out.println("-- Rejects / missing identity fields --");
                for (ParseOutcome.RejectedItem r : rejects) {
                    System.out.println(
                            "  wagon="
                                    + r.wagonNo()
                                    + " reason="
                                    + r.reasonCode()
                                    + " detail="
                                    + r.detail());
                }
            }
            if (!stored.isEmpty()) {
                System.out.println("-- Read-back current rows --");
                for (StagingRepository.StoredOperation s : stored) {
                    System.out.println(
                            "  "
                                    + s.wagonNo()
                                    + " rev="
                                    + s.revisionNo()
                                    + " utc="
                                    + s.operationTimeUtc()
                                    + " station="
                                    + s.stationCode()
                                    + " weight="
                                    + s.weightTons());
                }
            }
            System.out.println(
                    "NOTE event_key: changes to station_code / operation code / train_index "
                            + "create a NEW event row, not a revision. Weight/cargo edits keep the key.");
        }

        public ImportMode mode() {
            return mode;
        }

        public RunStatus parseStatus() {
            return parseStatus;
        }

        public RunStatus dbStatus() {
            return dbStatus;
        }

        public String errorSummary() {
            return errorSummary;
        }

        public Long runId() {
            return runId;
        }

        public int wagonsSeen() {
            return wagonsSeen;
        }

        public List<RwlOperation> operations() {
            return operations;
        }

        public List<ParseOutcome.RejectedItem> rejects() {
            return rejects;
        }

        public ImportCounters counters() {
            return counters;
        }

        public List<StagingRepository.StoredOperation> stored() {
            return stored;
        }

        public List<String> rawBodies() {
            return rawBodies;
        }

        public static final class Builder {
            private ImportMode mode;
            private String requestSummary;
            private RunStatus parseStatus;
            private RunStatus dbStatus;
            private String errorSummary;
            private Long runId;
            private int wagonsSeen;
            private List<RwlOperation> operations;
            private List<ParseOutcome.RejectedItem> rejects;
            private ImportCounters counters;
            private List<StagingRepository.StoredOperation> stored;
            private List<String> rawBodies;

            public Builder mode(ImportMode mode) {
                this.mode = mode;
                return this;
            }

            public Builder requestSummary(String requestSummary) {
                this.requestSummary = requestSummary;
                return this;
            }

            public Builder parseStatus(RunStatus parseStatus) {
                this.parseStatus = parseStatus;
                return this;
            }

            public Builder dbStatus(RunStatus dbStatus) {
                this.dbStatus = dbStatus;
                return this;
            }

            public Builder errorSummary(String errorSummary) {
                this.errorSummary = errorSummary;
                return this;
            }

            public Builder runId(Long runId) {
                this.runId = runId;
                return this;
            }

            public Builder wagonsSeen(int wagonsSeen) {
                this.wagonsSeen = wagonsSeen;
                return this;
            }

            public Builder operations(List<RwlOperation> operations) {
                this.operations = operations;
                return this;
            }

            public Builder rejects(List<ParseOutcome.RejectedItem> rejects) {
                this.rejects = rejects;
                return this;
            }

            public Builder counters(ImportCounters counters) {
                this.counters = counters;
                return this;
            }

            public Builder stored(List<StagingRepository.StoredOperation> stored) {
                this.stored = stored;
                return this;
            }

            public Builder rawBodies(List<String> rawBodies) {
                this.rawBodies = rawBodies;
                return this;
            }

            public ImportReport build() {
                return new ImportReport(this);
            }
        }
    }
}
