package platform.app.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void syntheticPlatformRuntimeIsSafeHonestyLabel() throws Exception {
        mockMvc.perform(get("/api/v1/platform/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSourceKind").value("SYNTHETIC"))
                .andExpect(jsonPath("$.displayLabel").value("Synthetic (offline)"))
                .andExpect(jsonPath("$.registryConnected").value(true))
                .andExpect(jsonPath("$.watchedConnected").value(true))
                .andExpect(jsonPath("$.jdbcUrl").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    @Test
    void syntheticCardAndDayFixtures() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/SYN-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wagonCode").value("SYN-00001"))
                .andExpect(jsonPath("$.currentState.stationCode").value("7100"))
                .andExpect(jsonPath("$.currentState.operationCode").value("05"))
                .andExpect(jsonPath("$.currentState.dislocationTime").value("2024-01-16T06:00:00"))
                .andExpect(jsonPath("$.currentState.trainIndex").value("TRN-DIFF"));

        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.lines[2].stationCode").value("6902"))
                .andExpect(jsonPath("$.lines[2].operationCode").value("01"))
                .andExpect(jsonPath("$.lines[2].dislocationTime").value("2024-01-15T23:59:59.5"));

        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(0)));
    }

    @Test
    void currentStateIsNotDerivedFromLastDayLine() throws Exception {
        String cardJson = mockMvc.perform(get("/api/v1/wagons/SYN-00001"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String dayJson = mockMvc.perform(
                        get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Current fixture: 7100/05/2024-01-16T06:00:00; last day line: 6902/01/23:59:59.500
        assertTrue(cardJson.contains("\"stationCode\":\"7100\""));
        assertTrue(cardJson.contains("\"operationCode\":\"05\""));
        assertTrue(dayJson.contains("\"stationCode\":\"6902\""));
        assertTrue(dayJson.contains("\"operationCode\":\"01\""));
        assertFalse(cardJson.contains("\"stationCode\":\"6902\""));
    }

    @Test
    void syntheticRegistryAndDrillDownCard() throws Exception {
        mockMvc.perform(get("/api/v1/wagons").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(250))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.content", hasSize(50)))
                .andExpect(jsonPath("$.content[0].wagonCode").value("SYN-00001"))
                .andExpect(jsonPath("$.content[0].model").value("12-132"))
                .andExpect(jsonPath("$.content[0].wagonKind").value("КРЫТЫЙ"))
                .andExpect(jsonPath("$.content[0].ownerName").value("SYN-OWNER"))
                .andExpect(jsonPath("$.content[0].homeStationCode").value("6900"))
                .andExpect(jsonPath("$.content[0].homeStationName").value("SYN-STATION"))
                .andExpect(jsonPath("$.content[0].inventoryPark").value(true));

        mockMvc.perform(get("/api/v1/wagons").param("search", "SYN-0025").param("sort", "code,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].wagonCode").value("SYN-00250"))
                .andExpect(jsonPath("$.content[0].model").exists());

        mockMvc.perform(get("/api/v1/wagons/SYN-00002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wagonCode").value("SYN-00002"))
                .andExpect(jsonPath("$.currentState").value(nullValue()));
    }

    @Test
    void syntheticWatchedListAndCardPathUnchanged() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/watched").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(60))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(50)))
                .andExpect(jsonPath("$.content[0].wagonCode").value("SYN-00001"))
                .andExpect(jsonPath("$.content[0].model").exists())
                .andExpect(jsonPath("$.membershipSource").value("FIXTURE"));

        mockMvc.perform(get("/api/v1/wagons/watched").param("page", "1").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.content[0].wagonCode").value("SYN-00051"));

        mockMvc.perform(get("/api/v1/wagons/watched").param("search", "NOMATCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/wagons/watched").param("search", "SYN-00250"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/wagons/watched").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PAGE"));

        mockMvc.perform(get("/api/v1/wagons/SYN-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wagonCode").value("SYN-00001"))
                .andExpect(jsonPath("$.model").exists());
    }
}
