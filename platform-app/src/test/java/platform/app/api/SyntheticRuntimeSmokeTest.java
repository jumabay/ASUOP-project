package platform.app.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * P0-5 in-process REST smoke for synthetic profile (R1–R3).
 * No Oracle/JDBC; safe for CI. Broader contract coverage lives in
 * {@link SyntheticWagonApiIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("synthetic")
@Tag("smoke")
@DisplayName("P0-5 synthetic REST smoke (R1–R3)")
class SyntheticRuntimeSmokeTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GET card SYN-00001")
    void card() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/SYN-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wagonCode").value("SYN-00001"));
    }

    @Test
    @DisplayName("GET day history 2024-01-15 (data) and 2024-01-16 (empty)")
    void dayHistory() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(0)));
    }

    @Test
    @DisplayName("GET registry page")
    void registry() throws Exception {
        mockMvc.perform(get("/api/v1/wagons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].wagonCode").exists());
    }

    @Test
    @DisplayName("GET watched list")
    void watched() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/watched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].wagonCode").exists());
    }
}
