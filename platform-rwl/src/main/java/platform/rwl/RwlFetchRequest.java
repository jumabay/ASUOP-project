package platform.rwl;

import java.util.Objects;

public final class RwlFetchRequest {

    private final RwlRequestType requestType;
    private final String summary;
    private final String format; // xml | json

    public RwlFetchRequest(RwlRequestType requestType, String summary, String format) {
        this.requestType = Objects.requireNonNull(requestType, "requestType");
        this.summary = summary == null ? "" : summary;
        this.format = format == null || format.isBlank() ? "xml" : format.trim().toLowerCase();
    }

    public RwlRequestType requestType() {
        return requestType;
    }

    public String summary() {
        return summary;
    }

    public String format() {
        return format;
    }
}
