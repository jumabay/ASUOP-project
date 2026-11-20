package platform.fleet;

/**
 * Read-only access to wagons on the watch list (R3).
 * Synthetic membership is fixture-based ({@link WatchedMembershipSource#FIXTURE});
 * Oracle GetActiveWagonList parity is not confirmed (G31 OPEN).
 * RAIL path returns empty with {@link WatchedMembershipSource#EMPTY_PENDING_G31}.
 */
public interface WatchedWagonPort {

    WatchedWagonPage findPage(WagonRegistryCriteria criteria);
}
