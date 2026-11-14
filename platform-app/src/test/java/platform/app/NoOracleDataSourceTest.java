package platform.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("synthetic")
class NoOracleDataSourceTest {

    @Autowired
    ApplicationContext context;

    @Test
    void syntheticProfileHasNoDataSourceBean() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(DataSource.class));
        assertFalse(context.containsBean("dataSource"));
    }
}
