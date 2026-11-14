package platform.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "platform",
        exclude = DataSourceAutoConfiguration.class)
public class RailwayPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RailwayPlatformApplication.class, args);
    }
}
