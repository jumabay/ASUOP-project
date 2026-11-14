package platform.dislocation;

import java.time.LocalDate;

public final class ResultLimitExceededException extends RuntimeException {

    private final String wagonCode;
    private final LocalDate date;
    private final int actualCount;
    private final int limit;

    public ResultLimitExceededException(String wagonCode, LocalDate date, int actualCount, int limit) {
        super("State report lines exceed limit " + limit + " (actual=" + actualCount + ")");
        this.wagonCode = wagonCode;
        this.date = date;
        this.actualCount = actualCount;
        this.limit = limit;
    }

    public String wagonCode() {
        return wagonCode;
    }

    public LocalDate date() {
        return date;
    }

    public int actualCount() {
        return actualCount;
    }

    public int limit() {
        return limit;
    }
}
