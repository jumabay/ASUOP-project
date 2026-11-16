package platform.intake;

import java.util.Objects;

public final class IntakeReject {

    private final String reasonCode;
    private final String wagonCode;
    private final String detail;

    public IntakeReject(String reasonCode, String wagonCode, String detail) {
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode");
        this.wagonCode = wagonCode == null ? "" : wagonCode;
        this.detail = detail == null ? "" : detail;
    }

    public String reasonCode() {
        return reasonCode;
    }

    public String wagonCode() {
        return wagonCode;
    }

    public String detail() {
        return detail;
    }
}
