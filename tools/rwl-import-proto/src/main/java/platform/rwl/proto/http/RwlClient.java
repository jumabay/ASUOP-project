package platform.rwl.proto.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import platform.rwl.proto.config.RwlConfig;

/** Read-only RWL client. Live prototype uses {@code view_vagon} only. */
public final class RwlClient {

    private final RwlConfig config;
    private final HttpClient http;

    public RwlClient(RwlConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.connectTimeoutMs()))
                .build();
    }

    public List<FetchedBody> fetchViewVagonBodies() throws IOException, InterruptedException {
        List<FetchedBody> bodies = new ArrayList<>();
        for (String vagonNo : config.vagonNos()) {
            String url = buildViewVagonUrl(vagonNo);
            String masked = UrlMasker.mask(url);
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofMillis(config.readTimeoutMs()))
                        .GET()
                        .header("Accept", "application/xml, text/xml, */*")
                        .build();
                HttpResponse<InputStream> response =
                        http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                byte[] bytes;
                try (InputStream in = response.body()) {
                    bytes = in.readAllBytes();
                }
                String body = new String(bytes, StandardCharsets.UTF_8);
                bodies.add(new FetchedBody(vagonNo, response.statusCode(), body, masked));
            } catch (IOException e) {
                throw new IOException("RWL network error for " + masked + ": " + UrlMasker.maskException(e), e);
            }
        }
        return bodies;
    }

    private String buildViewVagonUrl(String vagonNo) {
        return config.baseUrl()
                + "?name=" + enc(config.rwlName())
                + "&password=" + enc(config.rwlPassword())
                + "&request_type=view_vagon"
                + "&vagon_no=" + enc(vagonNo);
        // return_format omitted: documented default is XML
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    public record FetchedBody(String vagonNo, int httpStatus, String body, String maskedUrl) {
    }
}
