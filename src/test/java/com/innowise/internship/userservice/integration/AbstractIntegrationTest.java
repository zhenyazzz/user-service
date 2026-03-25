package com.innowise.internship.userservice.integration;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@ActiveProfiles("integrationtest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    protected WebTestClient webTestClient;

    @BeforeEach
    void setUpWebTestClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }

    protected HttpHeaders authHeaders(UUID userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        headers.set("X-User-Email", "user@example.com");
        headers.set("X-User-Roles", role != null ? role : "ROLE_USER");
        return headers;
    }

    protected HttpHeaders authHeaders(UUID userId) {
        return authHeaders(userId, "ROLE_USER");
    }

    protected HttpHeaders adminHeaders(UUID userId) {
        return authHeaders(userId, "ROLE_ADMIN");
    }

    protected void withAuth(HttpHeaders target, UUID userId) {
        authHeaders(userId).forEach((name, values) -> values.forEach(value -> target.add(name, value)));
    }

    protected void withAuth(HttpHeaders target, UUID userId, String role) {
        authHeaders(userId, role).forEach((name, values) -> values.forEach(value -> target.add(name, value)));
    }
}
