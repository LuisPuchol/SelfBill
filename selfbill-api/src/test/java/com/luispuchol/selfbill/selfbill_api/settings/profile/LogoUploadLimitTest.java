package com.luispuchol.selfbill.selfbill_api.settings.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

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

    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

    @Autowired
    private TestRestTemplate http;

    @Test
    void rejectsOver2MB() {
        ResponseEntity<JsonNode> tooBig = upload(padded(PNG_SIGNATURE, 2_500_000));
        assertThat(tooBig.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(tooBig.getBody().get("errorCode").asText()).isEqualTo("USER_PROFILE_LOGO_TOO_LARGE");

        assertThat(upload(padded(PNG_SIGNATURE, 1_500_000)).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void rejectsBytesThatAreNotAnImage() {
        ResponseEntity<JsonNode> zeros = upload(new byte[1000]);

        assertThat(zeros.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(zeros.getBody().get("errorCode").asText()).isEqualTo("USER_PROFILE_INVALID_LOGO_FORMAT");
    }

    @Test
    void storesTheTypeTheBytesHaveNotTheDeclaredOne() {
        byte[] jpeg = padded(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 }, 100);

        // Declared as logo.png (the multipart part is image/png by extension)
        assertThat(upload(jpeg).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(userProfileService.getLogo().contentType()).isEqualTo("image/jpeg");
    }

    private static byte[] padded(byte[] start, int size) {
        byte[] bytes = new byte[size];
        System.arraycopy(start, 0, bytes, 0, start.length);
        Arrays.fill(bytes, start.length, size, (byte) 1);
        return bytes;
    }

    private ResponseEntity<JsonNode> upload(byte[] bytes) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
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
