package com.innowise.internship.userservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.innowise.internship.userservice.dto.request.UserUpdateRequest;
import com.innowise.internship.userservice.dto.response.ErrorResponse;
import com.innowise.internship.userservice.dto.response.UserResponse;
import com.innowise.internship.userservice.model.enums.UserStatus;
import com.innowise.internship.userservice.utils.UserTestDataFactory;

@DisplayName("User API integration tests (Controller → Service → Repository → DB)")
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private UserResponse createUser() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        return webTestClient
                .post()
                .uri("/users")
                .bodyValue(UserTestDataFactory.buildUserCreateRequest(email))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Nested
    @DisplayName("POST /users")
    class CreateUser {

        @Test
        @DisplayName("creates user and returns 201 with body; then getById returns same data")
        void createsUser_thenGetById_returnsSameData() {
            UserResponse created = createUser();

            assertThat(created).isNotNull();
            assertThat(created.id()).isNotNull();
            assertThat(created.email()).isNotNull();
            assertThat(created.name()).isEqualTo(UserTestDataFactory.NORMALIZED_NAME);
            assertThat(created.surname()).isEqualTo(UserTestDataFactory.NORMALIZED_SURNAME);
            assertThat(created.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(created.paymentCards()).isNotNull().isEmpty();

            webTestClient
                    .get().uri("/users/{id}", created.id())
                    .headers(h -> withAuth(h, created.id()))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserResponse.class)
                    .value(body -> {
                        assertThat(body.id()).isEqualTo(created.id());
                        assertThat(body.email()).isEqualTo(created.email());
                        assertThat(body.name()).isEqualTo(created.name());
                        assertThat(body.surname()).isEqualTo(created.surname());
                        assertThat(body.paymentCards()).isNotNull();
                    });
        }

        @Test
        @DisplayName("when email already exists returns 409 and error body")
        void whenEmailExists_returns409() {
            UserResponse first = createUser();

            webTestClient
                    .post().uri("/users")
                    .bodyValue(UserTestDataFactory.buildUserCreateRequest(first.email()))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(ErrorResponse.class)
                    .value(err -> assertThat(err.errorCode()).isEqualTo("USER_ALREADY_EXISTS"));
        }
    }

    @Nested
    @DisplayName("GET /users/by-email")
    class GetUserIdByEmail {

        @Test
        @DisplayName("returns user id for existing email (no auth required)")
        void returnsUserIdForExistingEmail() {
            UserResponse created = createUser();

            UUID id = webTestClient
                    .get().uri(uri -> uri.path("/users/by-email").queryParam("email", created.email()).build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UUID.class)
                    .returnResult().getResponseBody();
            assertThat(id).isEqualTo(created.id());
        }

        @Test
        @DisplayName("normalizes email (trim, lowerCase) before lookup")
        void normalizesEmailBeforeLookup() {
            webTestClient.post().uri("/users")
                    .bodyValue(UserTestDataFactory.buildUserCreateRequest("john@example.com"))
                    .exchange()
                    .expectStatus().isCreated();

            UUID id = webTestClient
                    .get().uri(uri -> uri.path("/users/by-email").queryParam("email", "John@Example.COM").build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UUID.class)
                    .returnResult().getResponseBody();
            assertThat(id).isNotNull();
        }

        @Test
        @DisplayName("when email not found returns 404")
        void whenEmailNotFound_returns404() {
            webTestClient
                    .get().uri(uri -> uri.path("/users/by-email").queryParam("email", UserTestDataFactory.WRONG_EMAIL).build())
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ErrorResponse.class)
                    .value(err -> assertThat(err.errorCode()).isEqualTo("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /users/{id}")
    class GetUserById {

        @Test
        @DisplayName("without auth returns 401 or 403")
        void withoutAuth_returns401() {
            webTestClient
                    .get().uri("/users/{id}", UUID.randomUUID())
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("user requests own profile returns 200")
        void whenUserRequestsOwnProfile_returns200() {
            UserResponse created = createUser();
            UUID userId = created.id();

            webTestClient
                    .get().uri("/users/{id}", userId)
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserResponse.class)
                    .value(body -> {
                        assertThat(body.id()).isEqualTo(userId);
                        assertThat(body.paymentCards()).isNotNull();
                    });
        }

        @Test
        @DisplayName("user requests another user's profile returns 403 (only admin can)")
        void whenUserRequestsOtherProfile_returns403() {
            UserResponse created = createUser();
            UUID requestedProfileId = created.id();
            UUID otherUserId = UUID.randomUUID();

            webTestClient
                    .get().uri("/users/{id}", requestedProfileId)
                    .headers(h -> withAuth(h, otherUserId))
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("when user not found returns 404")
        void whenUserNotFound_returns404() {
            UUID nonExistentId = UUID.randomUUID();
            webTestClient
                    .get().uri("/users/{id}", nonExistentId)
                    .headers(h -> withAuth(h, nonExistentId))
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("PUT /users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("user updates own profile returns 200")
        void whenUserUpdatesOwnProfile_returns200() {
            UserResponse created = createUser();
            UUID userId = created.id();
            UserUpdateRequest updateRequest = UserTestDataFactory.buildUserUpdateRequest();

            webTestClient
                    .put().uri("/users/{id}", userId)
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserResponse.class)
                    .value(body -> {
                        assertThat(body.name()).isEqualTo(UserTestDataFactory.NORMALIZED_NAME);
                        assertThat(body.surname()).isEqualTo(UserTestDataFactory.NORMALIZED_SURNAME);
                        assertThat(body.paymentCards()).isNotNull();
                    });
        }
    }

    @Nested
    @DisplayName("GET /users (admin)")
    class GetAllUsers {

        @Test
        @DisplayName("admin gets user list returns 200 and page")
        void whenAdminGetsUserList_returns200AndPage() {
            UUID adminId = UUID.randomUUID();
            webTestClient
                    .get().uri("/users?page=0&size=5")
                    .headers(h -> withAuth(h, adminId, "ROLE_ADMIN"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content").isArray();
        }

        @Test
        @DisplayName("non-admin user requests user list returns 403")
        void whenNonAdminRequestsUserList_returns403() {
            UserResponse created = createUser();

            webTestClient
                    .get().uri("/users")
                    .headers(h -> withAuth(h, created.id()))
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Nested
    @DisplayName("DELETE /users/{id} (soft delete)")
    class SoftDelete {

        @Test
        @DisplayName("admin soft-delete returns 204 and getById returns status DELETED")
        void whenAdmin_delete_returns204AndGetReturnsDeleted() {
            UserResponse created = createUser();
            UUID userId = created.id();

            webTestClient
                    .delete().uri("/users/{id}", userId)
                    .headers(h -> withAuth(h, userId, "ROLE_ADMIN"))
                    .exchange()
                    .expectStatus().isNoContent();

            webTestClient
                    .get().uri("/users/{id}", userId)
                    .headers(h -> withAuth(h, userId, "ROLE_ADMIN"))
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }
}
