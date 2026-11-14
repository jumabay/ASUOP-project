package platform.fleet;

import java.util.Optional;

/**
 * Data-access port for wagon card (owned by fleet).
 */
public interface WagonCardPort {

    Optional<WagonCard> findByCode(String wagonCode);
}
