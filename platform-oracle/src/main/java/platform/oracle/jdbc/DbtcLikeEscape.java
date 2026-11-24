package platform.oracle.jdbc;

/**
 * Escape user literals for Oracle {@code LIKE … ESCAPE '\'} (P5-G31 search).
 */
public final class DbtcLikeEscape {

    private DbtcLikeEscape() {
    }

    public static String escapeLikeLiteral(String literal) {
        if (literal == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(literal.length() + 4);
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}