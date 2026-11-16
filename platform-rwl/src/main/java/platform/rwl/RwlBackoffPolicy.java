package platform.rwl;

import java.time.Duration;
import java.util.Objects;

/** Exponential backoff for future live retries. P2: unit-tested only; no network. */
public final class RwlBackoffPolicy {

    private final Duration initial;
    private final double multiplier;
    private final Duration maxDelay;
    private final int maxAttempts;

    public RwlBackoffPolicy(Duration initial, double multiplier, Duration maxDelay, int maxAttempts) {
        this.initial = Objects.requireNonNull(initial, "initial");
        this.multiplier = multiplier;
        this.maxDelay = Objects.requireNonNull(maxDelay, "maxDelay");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
    }

    public static RwlBackoffPolicy defaults() {
        return new RwlBackoffPolicy(Duration.ofMillis(200), 2.0, Duration.ofSeconds(5), 3);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public int maxConcurrentRequests() {
        return 1;
    }

    public Duration delayBeforeAttempt(int attemptIndexZeroBased) {
        if (attemptIndexZeroBased <= 0) {
            return Duration.ZERO;
        }
        double millis = initial.toMillis() * Math.pow(multiplier, attemptIndexZeroBased - 1);
        long capped = Math.min((long) millis, maxDelay.toMillis());
        return Duration.ofMillis(Math.max(0, capped));
    }
}
