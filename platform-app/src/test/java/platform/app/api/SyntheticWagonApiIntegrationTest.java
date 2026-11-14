package platform.app.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("synthetic")
class SyntheticWagonApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void syntheticCardAndDayFixtures() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/SYN-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wagonCode").value("SYN-00001"));

        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(0)));
    }
}
