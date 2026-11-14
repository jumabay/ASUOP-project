package platform.fleet;

public final class WagonNotFoundException extends RuntimeException {

    private final String wagonCode;

    public WagonNotFoundException(String wagonCode) {
        super("Wagon not found: " + wagonCode);
        this.wagonCode = wagonCode;
    }

    public String wagonCode() {
        return wagonCode;
    }
}
