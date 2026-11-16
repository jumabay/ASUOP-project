package platform.rwl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import platform.intake.ImportIntakePort;
import platform.intake.IntakeBatch;
import platform.intake.IntakeResult;
import platform.rwl.parse.ParseResult;
import platform.rwl.parse.ParseStatus;
import platform.rwl.parse.RwlJsonParser;
import platform.rwl.parse.RwlXmlParser;

/**
 * Offline orchestration: client → parser → normalize → ImportIntakePort.
 * Does not write to Oracle.
 */
public final class RwlOfflinePipeline {

    private final RwlApiClient client;
    private final RwlXmlParser xmlParser;
    private final RwlJsonParser jsonParser;
    private final RwlNormalizer normalizer;
    private final ImportIntakePort intakePort;

    public RwlOfflinePipeline(RwlApiClient client, ImportIntakePort intakePort) {
        this.client = Objects.requireNonNull(client, "client");
        this.intakePort = Objects.requireNonNull(intakePort, "intakePort");
        this.xmlParser = new RwlXmlParser();
        this.jsonParser = new RwlJsonParser();
        this.normalizer = new RwlNormalizer();
    }

    public IntakeResult run(RwlFetchRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        RwlFetchResponse response = client.fetch(request);
        Instant fetchedAt = Instant.now();

        ParseResult parsed;
        if ("json".equals(request.format())) {
            parsed = jsonParser.parse(response.body(), fetchedAt);
        } else {
            parsed = xmlParser.parse(response.body(), fetchedAt);
        }

        if (parsed.status() == ParseStatus.API_ERROR || parsed.status() == ParseStatus.PARSE_ERROR) {
            String key = intakeKey(request, response.body(), parsed.status().name());
            IntakeBatch failed = new IntakeBatch(
                    key,
                    RwlNormalizer.SOURCE_RWL,
                    request.requestType().wireName() + ":" + request.summary(),
                    java.util.List.of(),
                    java.util.List.of(new platform.intake.IntakeReject(
                            parsed.status().name().toLowerCase(),
                            "",
                            parsed.errorCode())));
            return new IntakeResult(
                    IntakeResult.Status.REJECTED, key, 0, failed.rejects().size());
        }

        RwlNormalizer.NormalizationOutcome norm =
                normalizer.normalize(parsed.operations(), parsed.rejects());
        String key = intakeKey(request, response.body(), "OK");
        IntakeBatch batch = new IntakeBatch(
                key,
                RwlNormalizer.SOURCE_RWL,
                request.requestType().wireName() + ":" + request.summary(),
                norm.events(),
                norm.rejects());
        return intakePort.accept(batch);
    }

    static String intakeKey(RwlFetchRequest request, String body, String statusTag) {
        String material = request.requestType().wireName()
                + "|"
                + request.summary()
                + "|"
                + request.format()
                + "|"
                + statusTag
                + "|"
                + sha256(body);
        return sha256(material);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
