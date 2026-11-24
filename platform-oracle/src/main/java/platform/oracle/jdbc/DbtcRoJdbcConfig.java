package platform.oracle.jdbc;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Time-boxed P5-E {@code dbtc-ro} DataSource. Credentials from env only.
 * Default profile remains {@code synthetic} (this config inactive).
 */
@Configuration
@Profile("dbtc-ro")
public class DbtcRoJdbcConfig {

    @Bean
    DataSource dbtcRoDataSource(
            @Value("${asuop.dbtc.jdbc-url:jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD}") String url,
            @Value("${asuop.dbtc.user:Dbtc}") String user,
            @Value("${asuop.dbtc.password:#{null}}") String password) {
        if (password == null || password.isBlank()) {
            password = System.getenv("ASUOP_ORACLE_PASSWORD");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ASUOP_ORACLE_PASSWORD / asuop.dbtc.password required for dbtc-ro");
        }
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("oracle.jdbc.OracleDriver");
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        java.util.Properties props = new java.util.Properties();
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "15000");
        props.setProperty("oracle.jdbc.ReadTimeout", "30000");
        ds.setConnectionProperties(props);
        return ds;
    }

    @Bean
    JdbcClient dbtcRoJdbcClient(DataSource dbtcRoDataSource) {
        return JdbcClient.create(dbtcRoDataSource);
    }
}
