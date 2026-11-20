package platform.rwl.proto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import platform.rwl.proto.config.EnvFileLoader;

class EnvFileLoaderTest {

    @TempDir
    Path temp;

    @Test
    void emptyValuesAreOmitted() throws Exception {
        Path file = temp.resolve("t.env");
        Files.writeString(file, "RWL_NAME=\nRWL_PASSWORD=secret\nRWL_VAGON_NOS=\n");
        Map<String, String> loaded = EnvFileLoader.load(file);
        assertFalse(loaded.containsKey("RWL_NAME"));
        assertFalse(loaded.containsKey("RWL_VAGON_NOS"));
        assertEquals("secret", loaded.get("RWL_PASSWORD"));
    }
}
