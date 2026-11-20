package platform.rwl.proto.http;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Masks credential query parameters in URLs and error messages. */
public final class UrlMasker {

    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)([?&](?:name|password|group_password|group_login)=)([^&]*)");

    private UrlMasker() {
    }

    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Matcher m = SENSITIVE.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "***"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String maskException(Throwable t) {
        if (t == null) {
            return null;
        }
        String msg = t.getMessage();
        if (msg == null) {
            return t.getClass().getSimpleName();
        }
        return t.getClass().getSimpleName() + ": " + mask(msg);
    }

    public static boolean looksLikeCredentialLeak(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("password=") && !lower.contains("password=***");
    }
}
