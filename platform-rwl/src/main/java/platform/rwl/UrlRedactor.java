package platform.rwl;

import java.util.regex.Pattern;

/** Redacts credentials from RWL URLs and exception messages. Never log raw passwords. */
public final class UrlRedactor {

    private static final Pattern SENSITIVE =
            Pattern.compile("(?i)(password|name|passwd|pwd)=([^&\\s]*)");

    private UrlRedactor() {}

    public static String redactUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return SENSITIVE.matcher(url).replaceAll("$1=****");
    }

    public static String redactMessage(String message) {
        if (message == null) {
            return "";
        }
        return SENSITIVE.matcher(message).replaceAll("$1=****");
    }
}
