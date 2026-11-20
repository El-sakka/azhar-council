package eg.azhar.archive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Runs every Flyway migration on an empty PostgreSQL and exercises the REST API. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ArchiveApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;

    MockHttpSession session;

    @BeforeEach
    void login() throws Exception {
        session = (MockHttpSession) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Admin\",\"password\":\"Admin 123\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void migrationsLoadTheWholeDocument() {
        assertThat(jdbc.queryForObject("select count(*) from archive_file", Long.class)).isEqualTo(237);
        assertThat(jdbc.queryForObject("select count(*) from archive_register", Long.class)).isEqualTo(60);
        assertThat(jdbc.queryForObject("select count(*) from subject", Long.class)).isEqualTo(44);
        assertThat(jdbc.queryForObject("select count(*) from subject_entry", Long.class)).isEqualTo(567);
        assertThat(jdbc.queryForObject("select count(*) from subject_entry where archive_file_id is null "
                + "and archive_register_id is null", Long.class)).isZero();
    }

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/files")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"Admin\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void searchesInventoryByTitleSubjectAndYear() throws Exception {
        mvc.perform(get("/api/files").session(session)).andExpect(jsonPath("$.total").value(237));
        // hamza-insensitive: "امتحانات" also finds "الإمتحانات"
        mvc.perform(get("/api/files").session(session).param("q", "امتحانات").param("field", "subject"))
                .andExpect(jsonPath("$.total", greaterThan(0)));
        mvc.perform(get("/api/files").session(session).param("q", "1930").param("field", "year"))
                .andExpect(jsonPath("$.items[*].title", hasItem(containsString("1930"))));
        mvc.perform(get("/api/registers").session(session).param("yearFrom", "1944").param("yearTo", "1945"))
                .andExpect(jsonPath("$.total", greaterThan(0)));
        mvc.perform(get("/api/files").session(session).param("q", "%")).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void browsesSubjectTreeAndTables() throws Exception {
        mvc.perform(get("/api/subjects").session(session))
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[9].name").value("هيئة إدارية"))
                .andExpect(jsonPath("$[9].children[5].children", hasSize(5)));
        Long id = jdbc.queryForObject("select id from subject where code = '10/6/3'", Long.class);
        mvc.perform(get("/api/subjects/{id}/entries", id).session(session))
                .andExpect(jsonPath("$.path").value("هيئة إدارية / استحقاقات مالية / مرتبات"))
                .andExpect(jsonPath("$.fileCount").value(9))
                .andExpect(jsonPath("$.registerCount").value(2));
        mvc.perform(get("/api/subjects/999999/entries").session(session)).andExpect(status().isNotFound());
    }

    @Test
    void exportsExcel() throws Exception {
        byte[] body = mvc.perform(get("/api/export/inventory").session(session).param("q", "1928"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(body).startsWith('P', 'K');   // xlsx is a zip archive
    }
}
