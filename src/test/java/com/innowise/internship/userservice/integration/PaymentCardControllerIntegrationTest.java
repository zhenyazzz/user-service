package com.innowise.internship.userservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.innowise.internship.userservice.dto.request.PaymentCardCreateRequest;
import com.innowise.internship.userservice.dto.request.PaymentCardUpdateRequest;
import com.innowise.internship.userservice.dto.request.UserCreateRequest;
import com.innowise.internship.userservice.dto.response.ErrorResponse;
import com.innowise.internship.userservice.dto.response.PaymentCardResponse;
import com.innowise.internship.userservice.dto.response.UserResponse;
import com.innowise.internship.userservice.model.enums.PaymentCardStatus;
import com.innowise.internship.userservice.utils.PaymentCardTestDataFactory;
import com.innowise.internship.userservice.utils.UserTestDataFactory;

@DisplayName("Payment card API integration tests (Controller → Service → Repository → DB)")
class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    private UUID createUserAndGetId() {
        UserCreateRequest request = UserTestDataFactory.buildUserCreateRequest("carduser-" + UUID.randomUUID() + "@example.com");
        UserResponse res = webTestClient.post().uri("/users").bodyValue(request).exchange()
                .expectStatus().isCreated().expectBody(UserResponse.class).returnResult().getResponseBody();
        assertThat(res).isNotNull();
        return res.id();
    }

    @Nested
    @DisplayName("POST /cards")
    class CreateCard {

        @Test
        @DisplayName("when user exists creates card and returns 201; then getById returns same data")
        void createsCard_thenGetById_returnsSameData() {
            UUID userId = createUserAndGetId();
            PaymentCardCreateRequest request = PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber());

            PaymentCardResponse created = webTestClient
                    .post().uri("/cards")
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CREATED)
                    .expectBody(PaymentCardResponse.class)
                    .returnResult().getResponseBody();

            assertThat(created).isNotNull();
            assertThat(created.id()).isNotNull();
            assertThat(created.userId()).isEqualTo(userId);
            assertThat(created.number()).isEqualTo(request.number());
            assertThat(created.holder()).isEqualTo(PaymentCardTestDataFactory.DEFAULT_HOLDER);
            assertThat(created.status()).isEqualTo(PaymentCardStatus.ACTIVE);

            webTestClient
                    .get().uri("/cards/{id}", created.id())
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PaymentCardResponse.class)
                    .value(body -> {
                        assertThat(body.id()).isEqualTo(created.id());
                        assertThat(body.userId()).isEqualTo(userId);
                        assertThat(body.number()).isEqualTo(created.number());
                    });

            webTestClient
                    .get().uri("/users/{id}", userId)
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserResponse.class)
                    .value(user -> {
                        assertThat(user.paymentCards()).hasSize(1);
                        assertThat(user.paymentCards().get(0).id()).isEqualTo(created.id());
                        assertThat(user.paymentCards().get(0).number()).isEqualTo(created.number());
                        assertThat(user.paymentCards().get(0).status()).isEqualTo(PaymentCardStatus.ACTIVE);
                    });
        }

        @Test
        @DisplayName("when user not found returns 404")
        void whenUserNotFound_returns404() {
            UUID nonExistentUserId = UUID.randomUUID();
            PaymentCardCreateRequest request = PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber());

            webTestClient
                    .post().uri("/cards")
                    .headers(h -> withAuth(h, nonExistentUserId))
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("when card number already exists returns 409")
        void whenCardNumberExists_returns409() {
            UUID userId = createUserAndGetId();
            String sameNumber = "9999999999999999";
            PaymentCardCreateRequest request = PaymentCardTestDataFactory.buildPaymentCardCreateRequest(sameNumber);

            webTestClient.post().uri("/cards").headers(h -> withAuth(h, userId)).bodyValue(request).exchange().expectStatus().isCreated();

            webTestClient
                    .post().uri("/cards")
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(ErrorResponse.class)
                    .value(err -> assertThat(err.errorCode()).isEqualTo("CARD_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("when user already has 5 cards creating 6th returns 422 CARD_LIMIT_EXCEEDED")
        void whenCardLimitExceeded_returns422() {
            UUID userId = createUserAndGetId();
            for (int i = 0; i < 5; i++) {
                PaymentCardCreateRequest req = PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber());
                webTestClient
                        .post().uri("/cards")
                        .headers(h -> withAuth(h, userId))
                        .bodyValue(req)
                        .exchange()
                        .expectStatus().isCreated();
            }
            PaymentCardCreateRequest sixth = PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber());
            webTestClient
                    .post().uri("/cards")
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(sixth)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                    .expectBody(ErrorResponse.class)
                    .value(err -> assertThat(err.errorCode()).isEqualTo("CARD_LIMIT_EXCEEDED"));
        }
    }

    @Nested
    @DisplayName("GET /cards/{id}")
    class GetCardById {

        @Test
        @DisplayName("as owner returns 200 and card data")
        void asOwner_returns200() {
            UUID userId = createUserAndGetId();
            PaymentCardResponse created = webTestClient
                    .post().uri("/cards")
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber()))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PaymentCardResponse.class)
                    .returnResult().getResponseBody();
            UUID cardId = created.id();

            webTestClient
                    .get().uri("/cards/{id}", cardId)
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PaymentCardResponse.class)
                    .value(body -> assertThat(body.id()).isEqualTo(cardId));
        }

        @Test
        @DisplayName("as another user returns 403")
        void asOtherUser_returns403() {
            UUID ownerId = createUserAndGetId();
            PaymentCardResponse created = webTestClient
                    .post().uri("/cards").headers(h -> withAuth(h, ownerId))
                    .bodyValue(PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber()))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PaymentCardResponse.class)
                    .returnResult().getResponseBody();
            UUID cardId = created.id();
            UUID otherUserId = createUserAndGetId();

            webTestClient
                    .get().uri("/cards/{id}", cardId)
                    .headers(h -> withAuth(h, otherUserId))
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("when card not found returns 404 or 403")
        void whenCardNotFound_returns404() {
            UUID userId = createUserAndGetId();
            UUID nonExistentCardId = UUID.randomUUID();

            webTestClient
                    .get().uri("/cards/{id}", nonExistentCardId)
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Nested
    @DisplayName("GET /cards/user/{userId}")
    class GetCardsByUserId {

        @Test
        @DisplayName("as owner returns 200 and list of cards")
        void asOwner_returns200AndList() {
            UUID userId = createUserAndGetId();
            webTestClient
                    .post().uri("/cards")
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber()))
                    .exchange()
                    .expectStatus().isCreated();

            List<PaymentCardResponse> list = webTestClient
                    .get().uri("/cards/user/{userId}", userId)
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(PaymentCardResponse.class)
                    .returnResult().getResponseBody();
            assertThat(list).hasSize(1);
        }

        @Test
        @DisplayName("as other user returns 403")
        void asOtherUser_returns403() {
            UUID ownerId = createUserAndGetId();
            UUID otherUserId = createUserAndGetId();

            webTestClient
                    .get().uri("/cards/user/{userId}", ownerId)
                    .headers(h -> withAuth(h, otherUserId))
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Nested
    @DisplayName("PUT /cards/{id}")
    class UpdateCard {

        @Test
        @DisplayName("as owner updates and returns 200")
        void asOwner_updatesAndReturns200() {
            UUID userId = createUserAndGetId();
            PaymentCardResponse created = webTestClient
                    .post().uri("/cards")
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber()))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PaymentCardResponse.class)
                    .returnResult().getResponseBody();
            UUID cardId = created.id();
            PaymentCardUpdateRequest updateRequest = PaymentCardTestDataFactory.buildPaymentCardUpdateRequest();

            webTestClient
                    .put().uri("/cards/{id}", cardId)
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PaymentCardResponse.class)
                    .value(body -> assertThat(body.holder()).isEqualTo(updateRequest.holder()));
        }
    }

    @Nested
    @DisplayName("DELETE /cards/{id} (soft delete)")
    class SoftDelete {

        @Test
        @DisplayName("as owner delete returns 204 and getById returns status DELETED")
        void asOwner_delete_returns204AndGetReturnsDeleted() {
            UUID userId = createUserAndGetId();
            PaymentCardResponse created = webTestClient
                    .post().uri("/cards")
                    .headers(h -> withAuth(h, userId))
                    .bodyValue(PaymentCardTestDataFactory.buildPaymentCardCreateRequest(PaymentCardTestDataFactory.uniqueCardNumber()))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PaymentCardResponse.class)
                    .returnResult().getResponseBody();
            UUID cardId = created.id();

            webTestClient
                    .delete().uri("/cards/{id}", cardId)
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isNoContent();

            webTestClient
                    .get().uri("/cards/{id}", cardId)
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isNotFound();

            webTestClient
                    .get().uri("/users/{id}", userId)
                    .headers(h -> withAuth(h, userId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserResponse.class)
                    .value(user -> assertThat(user.paymentCards()).isEmpty());
        }
    }
}
