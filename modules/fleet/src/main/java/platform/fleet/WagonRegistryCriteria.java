package platform.fleet;

import java.util.Objects;

public record WagonRegistryCriteria(
        String search,
        String owner,
        String kind,
        int page,
        int size,
        WagonRegistrySort sort
) {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public WagonRegistryCriteria {
        Objects.requireNonNull(sort, "sort");
        if (page < 0) {
            throw new InvalidPageException("page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidPageSizeException("size must be between 1 and " + MAX_SIZE);
        }
        search = blankToNull(search);
        owner = blankToNull(owner);
        kind = blankToNull(kind);
    }

    private static String blankToNull(String value) {
        if (value != null && value.isBlank()) {
            return null;
        }
        return value;
    }
}
