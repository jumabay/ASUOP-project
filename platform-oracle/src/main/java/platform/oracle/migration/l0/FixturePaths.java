package platform.oracle.migration.l0;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Resolves {@code fixtures/migration/v1} from repo root or module working directory. */
public final class FixturePaths {

    public static final String RELATIVE = "fixtures/migration/v1";

    private FixturePaths() {
    }

    public static Optional<Path> findV1() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path direct = cwd.resolve(RELATIVE);
        if (Files.isDirectory(direct)) {
            return Optional.of(direct);
        }
        Path parent = cwd.getParent();
        while (parent != null) {
            Path candidate = parent.resolve(RELATIVE);
            if (Files.isDirectory(candidate) && Files.isRegularFile(candidate.resolve("meta.json"))) {
                return Optional.of(candidate);
            }
            // Also accept empty-but-present pack dir while DB agent is writing.
            if (Files.isDirectory(candidate)) {
                Path withMeta = candidate.resolve("meta.json");
                if (Files.isRegularFile(withMeta)) {
                    return Optional.of(candidate);
                }
            }
            parent = parent.getParent();
        }
        // Walk up looking for gradle root marker + fixtures.
        Path probe = cwd;
        for (int i = 0; i < 6 && probe != null; i++) {
            Path candidate = probe.resolve(RELATIVE);
            if (Files.isDirectory(candidate) && Files.isRegularFile(candidate.resolve("meta.json"))) {
                return Optional.of(candidate);
            }
            if (Files.isRegularFile(probe.resolve("settings.gradle.kts"))) {
                Path fromRoot = probe.resolve(RELATIVE);
                if (Files.isDirectory(fromRoot)) {
                    return Optional.of(fromRoot);
                }
            }
            probe = probe.getParent();
        }
        return Optional.empty();
    }

    public static Path requireV1() {
        return findV1().orElseThrow(() -> new IllegalStateException(
                "Missing " + RELATIVE + " (expected golden pack from Database agent)"));
    }
}
