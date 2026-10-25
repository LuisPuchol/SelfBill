package com.luispuchol.selfbill.selfbill_api.settings.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

// MockMvc does not apply the multipart limit, so this goes through a real server
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LogoUploadLimitTest extends DomainIntegrationTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void rejectsOver2MB() {
        ResponseEntity<JsonNode> tooBig = upload(2_500_000);
        assertThat(tooBig.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(tooBig.getBody().get("errorCode").asText()).isEqualTo("USER_PROFILE_LOGO_TOO_LARGE");

        assertThat(upload(1_500_000).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<JsonNode> upload(int size) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(new byte[size]) {
            @Override
            public String getFilename() {
                return "logo.png";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return http.postForEntity("/api/user-profile/logo", new HttpEntity<>(body, headers), JsonNode.class);
    }
}
