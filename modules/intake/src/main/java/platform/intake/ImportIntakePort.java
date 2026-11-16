package platform.intake;

/**
 * Accepts a normalized intake batch. Implementations may be in-memory (P2) or RAILDB writers (later).
 * Must not depend on RWL types or Oracle drivers.
 */
public interface ImportIntakePort {

    IntakeResult accept(IntakeBatch batch);
}
