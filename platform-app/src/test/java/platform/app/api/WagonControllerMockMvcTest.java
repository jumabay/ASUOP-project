package platform.app.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import platform.dislocation.StateReportDayPort;
import platform.dislocation.StateReportLine;
import platform.fleet.WagonCard;
import platform.fleet.WagonCardPort;

@SpringBootTest
@AutoConfigureMockMvc
class WagonControllerMockMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WagonCardPort wagonCardPort;

    @MockitoBean
    StateReportDayPort stateReportDayPort;

    @Test
    void getWagonCard_ok() throws Exception {
        when(wagonCardPort.findByCode("SYN-00001")).thenReturn(Optional.of(sampleCard()));

        mockMvc.perform(get("/api/v1/wagons/SYN-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wagonCode").value("SYN-00001"))
                .andExpect(jsonPath("$.model").value("12-132"))
                .andExpect(jsonPath("$.inventoryPark").value(true));
    }

    @Test
    void getWagonCard_notFound() throws Exception {
        when(wagonCardPort.findByCode("SYN-UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/wagons/SYN-UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WAGON_NOT_FOUND"));
    }

    @Test
    void getStateReports_ok() throws Exception {
        when(wagonCardPort.findByCode("SYN-00001")).thenReturn(Optional.of(sampleCard()));
        when(stateReportDayPort.findLinesForDay(eq("SYN-00001"), eq(LocalDate.of(2024, 1, 15))))
                .thenReturn(List.of(
                        new StateReportLine(1, "SYN-00001", LocalDateTime.of(2024, 1, 15, 8, 0), "6900", "01"),
                        new StateReportLine(2, "SYN-00001", LocalDateTime.of(2024, 1, 15, 14, 30), "6901", "03")));

        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wagonCode").value("SYN-00001"))
                .andExpect(jsonPath("$.date").value("2024-01-15"))
                .andExpect(jsonPath("$.lines", hasSize(2)));
    }

    @Test
    void getStateReports_emptyDay() throws Exception {
        when(wagonCardPort.findByCode("SYN-00001")).thenReturn(Optional.of(sampleCard()));
        when(stateReportDayPort.findLinesForDay(eq("SYN-00001"), eq(LocalDate.of(2024, 1, 16))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(0)));
    }

    @Test
    void getStateReports_invalidDate() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "15-01-2024"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DATE"));
    }

    @Test
    void getStateReports_missingDate() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_DATE"));
    }

    @Test
    void getStateReports_wagonNotFound() throws Exception {
        when(wagonCardPort.findByCode("SYN-UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/wagons/SYN-UNKNOWN/state-reports").param("date", "2024-01-15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WAGON_NOT_FOUND"));
    }

    @Test
    void getStateReports_limitExceeded() throws Exception {
        when(wagonCardPort.findByCode("SYN-00001")).thenReturn(Optional.of(sampleCard()));
        List<StateReportLine> many = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            many.add(new StateReportLine(
                    i, "SYN-00001", LocalDateTime.of(2024, 1, 15, 1, 0), "6900", "01"));
        }
        when(stateReportDayPort.findLinesForDay(any(), any())).thenReturn(many);

        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports").param("date", "2024-01-15"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("RESULT_LIMIT_EXCEEDED"));
    }

    private static WagonCard sampleCard() {
        return new WagonCard(
                "SYN-00001", "12-132", "КРЫТЫЙ", "SYN-OWNER", "6900", "SYN-STATION", true);
    }
}
