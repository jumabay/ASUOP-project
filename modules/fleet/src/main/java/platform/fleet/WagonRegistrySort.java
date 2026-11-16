package platform.fleet;

public enum WagonRegistrySort {
    CODE_ASC,
    CODE_DESC;

    public static WagonRegistrySort fromParam(String sort) {
        if (sort == null || sort.isBlank() || "code,asc".equals(sort.trim())) {
            return CODE_ASC;
        }
        if ("code,desc".equals(sort.trim())) {
            return CODE_DESC;
        }
        throw new InvalidSortException("Unsupported sort: " + sort);
    }
}
