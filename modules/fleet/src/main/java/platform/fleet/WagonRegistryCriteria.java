package platform.fleet;

import java.util.Objects;

public record WagonRegistryCriteria(
        String search,
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
        if (search != null && search.isBlank()) {
            search = null;
        }
    }
}
