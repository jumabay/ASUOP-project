package platform.rwl.proto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import platform.rwl.proto.http.UrlMasker;

class UrlMaskerTest {

    @Test
    void masksNameAndPassword() {
        String raw =
                "https://www.railwagonlocation.com/xml/export.php?name=secret_user&password=secret_pass&request_type=view_vagon";
        String masked = UrlMasker.mask(raw);
        assertTrue(masked.contains("name=***"));
        assertTrue(masked.contains("password=***"));
        assertFalse(masked.contains("secret_user"));
        assertFalse(masked.contains("secret_pass"));
        assertFalse(UrlMasker.looksLikeCredentialLeak(masked));
    }
}
