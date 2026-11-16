package platform.rwl;

/** P2: live RWL HTTP is forbidden until a separate APPROVALS row. */
public final class LiveRwlDisabledException extends UnsupportedOperationException {

    public LiveRwlDisabledException() {
        super("Live RWL HTTP is disabled in P2 (DENIED UNTIL APPROVED)");
    }
}
