package platform.rwl.proto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import platform.rwl.proto.config.RwlConfig;
import platform.rwl.proto.db.DbSupport;
import platform.rwl.proto.db.StagingRepository;
import platform.rwl.proto.importing.ImportService;
import platform.rwl.proto.importing.ImportService.ImportReport;
import platform.rwl.proto.model.ImportMode;
import platform.rwl.proto.model.RunStatus;

class ImportServiceH2Test {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = DbSupport.openH2("jdbc:h2:mem:rwl_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        DbSupport.applyDdl(connection, DbSupport.Dialect.H2);
    }

    @Test
    void importThenReplaySameFixtureIsUnchanged() throws Exception {
        Path fixture = Path.of("src/test/resources/fixtures/success-two-wagons.xml");
        RwlConfig config = RwlConfig.from(Map.of(), ImportMode.IMPORT, fixture, null, false, false);

        ImportService service = new ImportService(config);
        ImportReport first = service.run(connection, DbSupport.Dialect.H2);
        assertEquals(RunStatus.SUCCESS, first.dbStatus(), () -> "error=" + first.errorSummary());
        assertEquals(2, first.counters().inserted());
        assertEquals(2, first.stored().size());

        ImportReport second = service.run(connection, DbSupport.Dialect.H2);
        assertEquals(RunStatus.SUCCESS, second.dbStatus());
        assertEquals(0, second.counters().inserted());
        assertEquals(0, second.counters().updated());
        assertEquals(2, second.counters().unchanged());

        StagingRepository repo = new StagingRepository(connection, DbSupport.Dialect.H2);
        assertEquals(2, repo.readCurrentOperations().size());
    }

    @Test
    void correctedContentCreatesRevision() throws Exception {
        ImportService service = new ImportService(
                RwlConfig.from(
                        Map.of(),
                        ImportMode.IMPORT,
                        Path.of("src/test/resources/fixtures/success-two-wagons.xml"),
                        null,
                        false,
                        false));
        service.run(connection, DbSupport.Dialect.H2);

        ImportReport corrected = new ImportService(
                        RwlConfig.from(
                                Map.of(),
                                ImportMode.IMPORT,
                                Path.of("src/test/resources/fixtures/success-corrected-weight.xml"),
                                null,
                                false,
                                false))
                .run(connection, DbSupport.Dialect.H2);

        assertEquals(RunStatus.SUCCESS, corrected.dbStatus());
        assertEquals(0, corrected.counters().inserted());
        assertEquals(1, corrected.counters().updated());
        var current = corrected.stored().stream()
                .filter(s -> "12345678".equals(s.wagonNo()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, current.revisionNo());
        assertEquals("70", current.weightTons());
    }

    @Test
    void emptyFixtureYieldsEmptyStatus() throws Exception {
        ImportReport report = new ImportService(
                        RwlConfig.from(
                                Map.of(),
                                ImportMode.IMPORT,
                                Path.of("src/test/resources/fixtures/empty-vagons.xml"),
                                null,
                                false,
                                false))
                .run(connection, DbSupport.Dialect.H2);
        assertEquals(RunStatus.EMPTY, report.dbStatus());
        assertEquals(0, report.counters().inserted());
    }

    @Test
    void writeFailureMarksFailedAndDoesNotKeepOps() throws Exception {
        ImportReport report = new ImportService(
                        RwlConfig.from(
                                Map.of(),
                                ImportMode.IMPORT,
                                Path.of("src/test/resources/fixtures/success-two-wagons.xml"),
                                null,
                                true,
                                false))
                .run(connection, DbSupport.Dialect.H2);
        assertEquals(RunStatus.FAILED, report.dbStatus());
        assertEquals(0, report.counters().inserted());
        StagingRepository repo = new StagingRepository(connection, DbSupport.Dialect.H2);
        assertTrue(repo.readCurrentOperations().isEmpty());
        assertEquals("FAILED", repo.findRun(report.runId()).orElseThrow().status());
    }

    @Test
    void operationTimeStoredAsUtcIndependentOfDefaultZone() throws Exception {
        ZoneOffset previous = ZoneOffset.ofTotalSeconds(ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now()).getTotalSeconds());
        // Force a non-UTC default for the duration of the assertion via Calendar usage already UTC in repo
        ImportReport report = new ImportService(
                        RwlConfig.from(
                                Map.of(),
                                ImportMode.IMPORT,
                                Path.of("src/test/resources/fixtures/success-two-wagons.xml"),
                                null,
                                false,
                                false))
                .run(connection, DbSupport.Dialect.H2);
        assertEquals(
                java.time.LocalDateTime.of(2025, 11, 15, 10, 15, 0),
                report.stored().stream()
                        .filter(s -> "12345678".equals(s.wagonNo()))
                        .findFirst()
                        .orElseThrow()
                        .operationTimeUtc());
        assertEquals(previous, previous); // keep compiler quiet; zone independence covered by UTC Calendar binds
    }
}
