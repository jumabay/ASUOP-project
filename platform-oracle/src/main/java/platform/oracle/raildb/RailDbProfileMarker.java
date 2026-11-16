package platform.oracle.raildb;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Marks that profile {@code raildb-test} is recognized by the application context.
 * Intentionally empty: no DataSource and no Oracle client until a later approved stage.
 */
@Configuration
@Profile("raildb-test")
public class RailDbProfileMarker {
}
