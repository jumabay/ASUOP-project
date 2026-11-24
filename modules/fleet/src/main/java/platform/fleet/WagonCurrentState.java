package platform.fleet;

/**
 * Optional current dislocation snapshot for a wagon card (P5-C).
 *
 * <p>Loaded from a separate synthetic source — not derived from day-history lines.
 * All fields may be null/blank when unknown.
 */
public record WagonCurrentState(
        String stationCode,
        String operationCode,
        String dislocationTime,
        String trainIndex
) {
}
