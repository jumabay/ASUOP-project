package platform.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Default runtime is profile {@code synthetic} (no Oracle).
 * {@link DataSourceAutoConfiguration} stays excluded until an approved stage enables
 * profile {@code raildb-test} with an explicit RAILDB DataSource (not RAC {@code RAIL_APP}).
 */
@SpringBootApplication(
        scanBasePackages = "platform",
        exclude = DataSourceAutoConfiguration.class)
public class RailwayPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RailwayPlatformApplication.class, args);
    }
}
