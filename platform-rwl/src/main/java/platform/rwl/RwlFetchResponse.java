package platform.rwl;

import java.util.Objects;

public final class RwlFetchResponse {

    private final int httpStatus;
    private final String body;
    private final String redactedUrl;

    public RwlFetchResponse(int httpStatus, String body, String redactedUrl) {
        this.httpStatus = httpStatus;
        this.body = Objects.requireNonNull(body, "body");
        this.redactedUrl = redactedUrl == null ? "" : redactedUrl;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String body() {
        return body;
    }

    public String redactedUrl() {
        return redactedUrl;
    }
}
