package platform.oracle.dict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlScriptParserTest {

    @TempDir
    Path dir;

    @Test
    void parsesSelectsIgnoresComments() throws Exception {
        Path f = dir.resolve("G0_demo.sql");
        Files.writeString(f, """
                -- comment
                SELECT 1 FROM dual;
                /* block */
                SELECT 2 FROM dual;
                """);
        List<SqlScriptParser.Statement> stmts = SqlScriptParser.parseFile(f);
        assertEquals(2, stmts.size());
        assertTrue(stmts.get(0).sql().toUpperCase().startsWith("SELECT"));
    }

    @Test
    void rejectsNonSelect() throws Exception {
        Path f = dir.resolve("bad.sql");
        Files.writeString(f, "DELETE FROM dual;");
        assertThrows(IllegalStateException.class, () -> SqlScriptParser.parseFile(f));
    }
}
