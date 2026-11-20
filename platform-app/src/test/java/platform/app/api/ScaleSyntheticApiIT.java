package platform.app.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import platform.dislocation.DayStateReportQuery;
import platform.dislocation.StateReportDayPort;
import platform.dislocation.StateReportLine;
import platform.oracle.synthetic.SyntheticDayLineGenerator;
import platform.oracle.synthetic.SyntheticStateReportDayAdapter;

/**
 * HTTP-level scale checks for synthetic API (T1 validation boundary + T3 422).
 *
 * <p>Uses default T0 catalog (250) for Spring context; T1 large-catalog coverage lives in
 * {@code platform-oracle} {@code @Tag("scale")} unit tests (generated catalog, no 30k JSON).
 *
 * <p>Run: {@code .\scripts\gradle-jdk21.ps1 :platform-app:testScale}
 *
 * <p>Synthetic ≠ Oracle proof.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("synthetic")
@Tag("scale")
class ScaleSyntheticApiIT {

    private static final LocalDate OVERLIMIT_DATE = LocalDate.of(2024, 1, 18);

    @Autowired
    MockMvc mockMvc;

    @Test
    void t1_size201RejectedAsInvalidSize() throws Exception {
        mockMvc.perform(get("/api/v1/wagons").param("size", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SIZE"));
    }

    @Test
    void t1_watchedRemains60OnT0Catalog() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/watched").param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(60))
                .andExpect(jsonPath("$.content", hasSize(60)));
    }

    @Test
    void t3_day501Returns422ResultLimitExceeded() throws Exception {
        mockMvc.perform(get("/api/v1/wagons/SYN-00001/state-reports")
                        .param("date", OVERLIMIT_DATE.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("RESULT_LIMIT_EXCEEDED"));
    }

    @TestConfiguration
    static class OverLimitDayConfig {

        @Bean
        @Primary
        StateReportDayPort overLimitDayPort(ObjectMapper mapper) {
            List<StateReportLine> lines = SyntheticDayLineGenerator.generate(
                    "SYN-00001", OVERLIMIT_DATE, DayStateReportQuery.MAX_LINES + 1);
            return new SyntheticStateReportDayAdapter(
                    mapper,
                    Map.of(SyntheticStateReportDayAdapter.overlayKey("SYN-00001", OVERLIMIT_DATE), lines));
        }
    }
}
