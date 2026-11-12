package ru.student.fleet;

import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:fleet-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed-demo=false"
})
@AutoConfigureMockMvc
@Import(FleetIntegrationTest.FixedClock.class)
class FleetIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);
    @TestConfiguration static class FixedClock {
        // По UTC ещё 11 ноября, по Москве уже 12-е.
        @Bean @Primary Clock testClock() { return Clock.fixed(Instant.parse("2026-11-11T22:30:00Z"), ZoneId.of("Europe/Moscow")); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CarRepository repository;
    @BeforeEach void clear() { repository.deleteAll(); }

    private String data(String brand, Integer year, LocalDate day, String owner) throws Exception {
        return json.writeValueAsString(new CarRequest(brand, year, day, owner));
    }
    private long create(String brand, int year, LocalDate day, String owner) throws Exception {
        MvcResult result = mvc.perform(post("/api/cars").contentType(MediaType.APPLICATION_JSON).content(data(brand, year, day, owner)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("id").asLong();
    }
    private JsonNode getJson(MockHttpServletRequestBuilder request) throws Exception {
        return json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
    @Test void fullCrudPersistsAllFieldsAndCyrillicBrand() throws Exception {
        long id = create("  Лада  ", 2020, TODAY, "  Петров Павел Андреевич  ");
        mvc.perform(get("/api/cars/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.марка").value("Лада"))
                .andExpect(jsonPath("$.ownerName").value("Петров Павел Андреевич"));
        mvc.perform(put("/api/cars/{id}", id).contentType(MediaType.APPLICATION_JSON).content(data("ГАЗ", 2019, TODAY.minusDays(1), "Иванов Иван")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productionYear").value(2019));
        Car saved = repository.findById(id).orElseThrow();
        assertThat(saved.getМарка()).isEqualTo("ГАЗ");
        assertThat(saved.getRegisteredOn()).isEqualTo(TODAY.minusDays(1));
        mvc.perform(delete("/api/cars/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/cars/{id}", id)).andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }
    @Test void sortsByYearThenRussianBrandAndStableId() throws Exception {
        create("УАЗ", 2021, TODAY, "Петров");
        long firstLada = create("Лада", 2021, TODAY, "Петров");
        create("Toyota", 2020, TODAY, "Петров");
        long secondLada = create("Лада", 2021, TODAY, "Иванов");
        JsonNode cars = getJson(get("/api/cars").param("sorted", "true")).get("cars");
        assertThat(cars.findValuesAsText("марка")).containsExactly("Toyota", "Лада", "Лада", "УАЗ");
        assertThat(cars.get(1).get("id").asLong()).isEqualTo(firstLada);
        assertThat(cars.get(2).get("id").asLong()).isEqualTo(secondLada);
    }
    @Test void combinesSearchFieldsAndKeepsTotalCount() throws Exception {
        long id = create("Лада", 2021, TODAY, "Петров Павел");
        create("Лада", 2020, TODAY, "Иванов Иван");
        JsonNode result = getJson(get("/api/cars").param("марка", "ЛАДА").param("owner", "петров").param("id", String.valueOf(id))
                .param("yearFrom", "2021").param("yearTo", "2021").param("from", TODAY.toString()).param("to", TODAY.toString()));
        assertThat(result.get("cars").size()).isEqualTo(1);
        assertThat(result.get("total").asInt()).isEqualTo(2);
        assertThat(getJson(get("/api/cars").param("owner", "Несуществующий")).get("cars").size()).isZero();
    }
    @Test void searchEscapesSqlLikeWildcards() throws Exception {
        create("Тест%_!", 2020, TODAY, "Петров");
        create("Toyota", 2020, TODAY, "Петров");
        assertThat(getJson(get("/api/cars").param("марка", "%_!")).get("cars").size()).isEqualTo(1);
    }
    @Test void dateAndYearFiltersIncludeBoundaries() throws Exception {
        create("Toyota", 2019, TODAY.minusDays(10), "Петров");
        create("Лада", 2020, TODAY.minusDays(5), "Петров");
        create("УАЗ", 2021, TODAY, "Петров");
        assertThat(getJson(get("/api/cars").param("yearFrom", "2019").param("yearTo", "2020")).get("cars").size()).isEqualTo(2);
        assertThat(getJson(get("/api/cars").param("from", TODAY.minusDays(10).toString()).param("to", TODAY.minusDays(5).toString())).get("cars").size()).isEqualTo(2);
    }
    @Test void histogramHas30DaysZeroesAndMoscowDateBounds() throws Exception {
        create("Начало", 2020, TODAY.minusDays(29), "Петров");
        create("Сегодня 1", 2020, TODAY, "Петров");
        create("Сегодня 2", 2020, TODAY, "Петров");
        create("До периода", 2020, TODAY.minusDays(30), "Петров");
        JsonNode result = getJson(get("/api/cars/histogram"));
        assertThat(result.get("days").size()).isEqualTo(30);
        assertThat(result.get("from").asText()).isEqualTo("2026-10-14");
        assertThat(result.get("to").asText()).isEqualTo("2026-11-12");
        assertThat(result.get("total").asInt()).isEqualTo(3);
        assertThat(result.get("days").get(0).get("count").asInt()).isEqualTo(1);
        assertThat(result.get("days").get(1).get("count").asInt()).isZero();
        assertThat(result.get("days").get(29).get("count").asInt()).isEqualTo(2);
        assertThat(getJson(get("/api/cars/today")).asText()).isEqualTo("2026-11-12");
    }
    @Test void histogramChangesAfterCreateUpdateAndDelete() throws Exception {
        long id = create("Toyota", 2020, TODAY, "Петров");
        assertThat(getJson(get("/api/cars/histogram")).get("total").asInt()).isEqualTo(1);
        mvc.perform(put("/api/cars/{id}", id).contentType(MediaType.APPLICATION_JSON).content(data("Toyota", 2020, TODAY.minusDays(30), "Петров"))).andExpect(status().isOk());
        assertThat(getJson(get("/api/cars/histogram")).get("total").asInt()).isZero();
        mvc.perform(delete("/api/cars/{id}", id)).andExpect(status().isNoContent());
        assertThat(getJson(get("/api/cars")).get("total").asInt()).isZero();
    }
    @Test void rejectsMissingRequiredFieldsAndWhitespace() throws Exception {
        for (String payload : new String[]{data(" ", 2020, TODAY, "Петров"), data("Toyota", null, TODAY, "Петров"),
                data("Toyota", 2020, null, "Петров"), data("Toyota", 2020, TODAY, " ")}) {
            mvc.perform(post("/api/cars").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
        }
        assertThat(repository.count()).isZero();
    }
    @Test void rejectsInvalidYearAndDateRelationships() throws Exception {
        for (String payload : new String[]{data("Toyota", 1885, TODAY, "Петров"), data("Toyota", 2027, TODAY, "Петров"),
                data("Toyota", 2020, TODAY.plusDays(1), "Петров"), data("Toyota", 2020, LocalDate.of(2019, 12, 31), "Петров")}) {
            mvc.perform(post("/api/cars").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
        }
        assertThat(repository.count()).isZero();
    }
    @Test void invalidUpdateLeavesOriginalRecord() throws Exception {
        long id = create("Лада", 2020, TODAY, "Петров");
        mvc.perform(put("/api/cars/{id}", id).contentType(MediaType.APPLICATION_JSON).content(data("ГАЗ", 2027, TODAY, "Иванов"))).andExpect(status().isBadRequest());
        Car saved = repository.findById(id).orElseThrow();
        assertThat(saved.getМарка()).isEqualTo("Лада"); assertThat(saved.getOwnerName()).isEqualTo("Петров");
    }
    @Test void rejectsBadRangesAndMalformedTypes() throws Exception {
        mvc.perform(get("/api/cars").param("yearFrom", "2022").param("yearTo", "2020")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cars").param("from", "2026-11-12").param("to", "2026-11-10")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cars").param("from", "bad-date")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/cars").contentType(MediaType.APPLICATION_JSON).content("{bad}")).andExpect(status().isBadRequest());
        String floatYear = data("Toyota", 2020, TODAY, "Петров").replace("2020", "2020.5");
        mvc.perform(post("/api/cars").contentType(MediaType.APPLICATION_JSON).content(floatYear)).andExpect(status().isBadRequest());
    }
    @Test void missingCarReturns404ForAllOperations() throws Exception {
        mvc.perform(get("/api/cars/999999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/cars/999999").contentType(MediaType.APPLICATION_JSON).content(data("Toyota", 2020, TODAY, "Петров"))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/cars/999999")).andExpect(status().isNotFound());
    }
}
