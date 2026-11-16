package platform.rwl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import platform.intake.ImportIntakePort;
import platform.intake.IntakeBatch;
import platform.intake.IntakeResult;

/** P2: stores accepted intake keys in memory. No Oracle. */
public final class InMemoryImportIntake implements ImportIntakePort {

    private final Map<String, IntakeBatch> accepted = new LinkedHashMap<>();

    @Override
    public IntakeResult accept(IntakeBatch batch) {
        Objects.requireNonNull(batch, "batch");
        if (accepted.containsKey(batch.intakeKey())) {
            IntakeBatch prior = accepted.get(batch.intakeKey());
            return new IntakeResult(
                    IntakeResult.Status.DUPLICATE,
                    batch.intakeKey(),
                    prior.events().size(),
                    prior.rejects().size());
        }
        accepted.put(batch.intakeKey(), batch);
        return new IntakeResult(
                IntakeResult.Status.ACCEPTED,
                batch.intakeKey(),
                batch.events().size(),
                batch.rejects().size());
    }

    public int size() {
        return accepted.size();
    }
}
