package com.jailbird.textgeneration;

import com.jailbird.textgeneration.service.TextGenerationBusinessLogic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TextGenerationApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TextGenerationBusinessLogic businessLogic;

    @Test
    void contextLoads() {
        assertThat(businessLogic).isNotNull();
    }

    @Test
    void actuatorHealthEndpointWorks() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            Map.class);
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void businessLogicGeneratesTextCorrectly() {
        String result = businessLogic.generateText("Hello", null);
        assertThat(result).isNotEmpty();
        assertThat(result.toLowerCase()).contains("hello");
    }
}