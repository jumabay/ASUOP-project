package platform.rwl.proto.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Explicit KEY=VALUE file loader. Java does not read {@code .env} automatically;
 * values are applied only when the CLI is started with {@code --env-file=...}.
 */
public final class EnvFileLoader {

    private EnvFileLoader() {
    }

    public static Map<String, String> load(Path path) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            // Empty values are treated as unset (do not override process env with blank).
            if (value.isEmpty()) {
                continue;
            }
            values.put(key, value);
        }
        return values;
    }

    /** Puts loaded values into process environment view used by {@link System#getenv()} via overlay map. */
    public static Map<String, String> mergeWithProcessEnv(Map<String, String> fileValues) {
        Map<String, String> merged = new LinkedHashMap<>(System.getenv());
        merged.putAll(fileValues);
        return merged;
    }
}
