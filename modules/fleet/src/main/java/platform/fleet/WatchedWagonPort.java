package platform.fleet;

/**
 * Read-only access to wagons on the watch list (R3).
 * Synthetic membership is fixture-based; Oracle GetActiveWagonList parity is not confirmed.
 */
public interface WatchedWagonPort {

    WatchedWagonPage findPage(WagonRegistryCriteria criteria);
}
