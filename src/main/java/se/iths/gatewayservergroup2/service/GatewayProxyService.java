package se.iths.gatewayservergroup2.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.iths.gatewayservergroup2.dto.AuthServiceResponse;
import se.iths.gatewayservergroup2.dto.LoginResponse;
import com.fasterxml.jackson.core.JacksonException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GatewayProxyService {
    private final String authServerUrl;
    private final String productServiceUrl;
    private final String orderServiceUrl;
    private final String paymentServiceUrl;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final HttpClient httpClient;

    public GatewayProxyService(
            @Value("${app.auth-server-url}") String authServerUrl,
            @Value("${app.product-service-url}") String productServiceUrl,
            @Value("${app.order-service-url}") String orderServiceUrl,
            @Value("${app.payment-service-url}") String paymentServiceUrl,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}") String cookieSameSite,
            HttpClient httpClient) {
        this.authServerUrl = authServerUrl;
        this.productServiceUrl = productServiceUrl;
        this.orderServiceUrl = orderServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.httpClient = httpClient;
    }

    public ResponseEntity<String> createOrder(
            String requestBody,
            String authorization,
            String contentType) {
        return forwardPost(
                orderServiceUrl,
                "/order",
                requestBody,
                authorization,
                contentType
        );

    }

    public ResponseEntity<String> createPaymentCheckout(
            String requestBody,
            String authorization,
            String contentType
    ) {
        return forwardPost(
                paymentServiceUrl,
                "/payment/checkout",
                requestBody,
                authorization,
                contentType
        );
    }

    /* HJÄLPMETODER */
    private ResponseEntity<String> forwardPost(
            String baseUrl,
            String path,
            String requestBody,
            String authorization,
            String contentType) {

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        return send(builder, authorization);
    }

    private ResponseEntity<String> send(HttpRequest.Builder builder, String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            builder.header(
                    HttpHeaders.AUTHORIZATION,
                    authorization
            );
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.statusCode());

            response.headers()
                    .firstValue(HttpHeaders.CONTENT_TYPE)
                    .ifPresent(contentType -> responseBuilder.header(
                            HttpHeaders.CONTENT_TYPE,
                            contentType
                    ));
            return responseBuilder.body(response.body());
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Kunde inte nå bakomliggande tjänst");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Anropet avbröts");
        }
    }
// =========================== Product Service Methods ===========================

    public ResponseEntity<String> getAllProducts(String authorization) {
        return forwardGet(productServiceUrl, "/api/products", authorization);
    }

    public ResponseEntity<String> getProductById(Long id, String authorization) {
        return forwardGet(productServiceUrl, "/api/products/" + id, authorization);
    }

    public ResponseEntity<String> addProduct(String requestBody, String authorization, String contentType) {
        return forwardPost(productServiceUrl, "/api/products/add", requestBody, authorization, contentType);
    }

    public ResponseEntity<String> deleteProduct(Long id, String authorization) {
        return forwardDelete(productServiceUrl, "/api/products/delete/" + id, authorization);
    }

    private ResponseEntity<String> forwardGet(
            String baseUrl,
            String path,
            String authorization) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .GET();
        return send(builder, authorization);
    }

    private ResponseEntity<String>
    forwardDelete(
            String baseUrl,
            String path,
            String authorization) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .DELETE();
        return send(builder, authorization);
    }
// ================================================ AUTH =========================================

    public ResponseEntity<?> loginWithCookie(String requestBody, String contentType) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(authServerUrl + "/auth/login"))
                .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        return sendLogin(request);
    }

    public ResponseEntity<String> register(String requestBody, String contentType) {
        return forwardPost(authServerUrl, "/auth/register", requestBody, null, contentType);
    }

    public ResponseEntity<String> getJwks() {
        return forwardGet(authServerUrl, "/auth/jwks", null);
    }

    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    // Hjälpmetoder för login-förfrågan med cookie

    private ResponseEntity<?> sendLogin(HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResponseEntity.status(response.statusCode()).body(response.body());
            }
            return createLoginResponse(response.body());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Gateway could not reach auth service");
        }
    }

    private ResponseEntity<?> createLoginResponse(String responseBody) {
        try {
            AuthServiceResponse authResponse = objectMapper.readValue(
                    responseBody,
                    AuthServiceResponse.class
            );
            long maxAgeSeconds = (authResponse.expiresIn() != null && authResponse.expiresIn() > 0)
                    ? authResponse.expiresIn()
                    : 86400;

            ResponseCookie cookie = ResponseCookie.from("access_token", authResponse.accessToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .path("/")
                    .maxAge(Duration.ofSeconds(maxAgeSeconds))
                    .build();

            LoginResponse loginResponse = new LoginResponse(
                    authResponse.expiresIn(),
                    authResponse.subject(),
                    authResponse.roles()
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(loginResponse);
        } catch (JacksonException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Auth service returned an invalid response");
        }
    }

//===================================================================================================

}
