package platform.rwl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import platform.intake.IntakeResult;

class RwlOfflinePipelineTest {

    @Test
    void xmlFixtureAcceptedAndIdempotent() throws Exception {
        InMemoryImportIntake intake = new InMemoryImportIntake();
        RwlOfflinePipeline pipeline =
                new RwlOfflinePipeline(new FixtureRwlApiClient("rwl/fixtures/success-two-wagons.xml"), intake);

        RwlFetchRequest request =
                new RwlFetchRequest(RwlRequestType.GET_USER_VAGONS, "fixture-two", "xml");
        IntakeResult first = pipeline.run(request);
        IntakeResult second = pipeline.run(request);

        assertEquals(IntakeResult.Status.ACCEPTED, first.status());
        assertEquals(2, first.eventCount());
        assertEquals(IntakeResult.Status.DUPLICATE, second.status());
        assertEquals(1, intake.size());
    }

    @Test
    void jsonFixtureProvisionalAccepted() throws Exception {
        InMemoryImportIntake intake = new InMemoryImportIntake();
        RwlOfflinePipeline pipeline =
                new RwlOfflinePipeline(new FixtureRwlApiClient("rwl/fixtures/success-one-wagon.json"), intake);

        IntakeResult result = pipeline.run(
                new RwlFetchRequest(RwlRequestType.VIEW_VAGON, "fixture-json", "json"));
        assertEquals(IntakeResult.Status.ACCEPTED, result.status());
        assertEquals(1, result.eventCount());
    }

    @Test
    void apiErrorDoesNotAcceptBatch() throws Exception {
        InMemoryImportIntake intake = new InMemoryImportIntake();
        RwlOfflinePipeline pipeline =
                new RwlOfflinePipeline(new FixtureRwlApiClient("rwl/fixtures/api-error-plain.txt"), intake);

        IntakeResult result = pipeline.run(
                new RwlFetchRequest(RwlRequestType.VIEW_VAGON, "err", "xml"));
        assertEquals(IntakeResult.Status.REJECTED, result.status());
        assertEquals(0, intake.size());
    }

    @Test
    void liveClientDisabled() {
        assertThrows(LiveRwlDisabledException.class, () -> new LiveRwlApiClient()
                .fetch(new RwlFetchRequest(RwlRequestType.VIEW_VAGON, "x", "xml")));
    }

    @Test
    void urlRedactorHidesPassword() {
        String redacted = UrlRedactor.redactUrl(
                "https://example/xml/export.php?name=user&password=super-secret&request_type=view_vagon");
        assertTrue(redacted.contains("password=****"));
        assertTrue(redacted.contains("name=****"));
        assertTrue(!redacted.contains("super-secret"));
    }

    @Test
    void backoffIncreasesThenCaps() {
        RwlBackoffPolicy policy = RwlBackoffPolicy.defaults();
        assertEquals(0, policy.delayBeforeAttempt(0).toMillis());
        assertTrue(policy.delayBeforeAttempt(1).toMillis() >= 200);
        assertTrue(policy.delayBeforeAttempt(10).toMillis() <= 5000);
        assertEquals(1, policy.maxConcurrentRequests());
    }
}
