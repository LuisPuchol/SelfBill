package com.luispuchol.selfbill.selfbill_api.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SpaWebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesIndexAtRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("selfbill-test-index")))
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    void servesIndexForAppRoutes() throws Exception {
        mockMvc.perform(get("/delivery-notes/12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("selfbill-test-index")));
    }

    @Test
    void servesBuildFiles() throws Exception {
        mockMvc.perform(get("/_app/immutable/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("selfbill-test-asset")))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")));
    }

    @Test
    void missingFileIsNotIndex() throws Exception {
        mockMvc.perform(get("/_app/immutable/missing.js"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownApiRouteIsNotIndex() throws Exception {
        mockMvc.perform(get("/api/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("selfbill-test-index"))));
    }

    @Test
    void keepsApiDocsAndSwagger() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void logsOnlyApiRequests(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/_app/immutable/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/api/articles/next-code")).andExpect(status().isOk());

        assertThat(output.getAll())
                .contains("GET /api/articles/next-code")
                .doesNotContain("GET /_app/immutable/app.js");
    }
}
