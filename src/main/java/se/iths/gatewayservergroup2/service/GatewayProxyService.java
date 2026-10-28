package se.iths.gatewayservergroup2.service;

import com.fasterxml.jackson.core.JacksonException;
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service responsible for proxying incoming gateway requests to the appropriate backend microservices
 * (Auth, Product, Order, Payment) using Java's standard {@link HttpClient}.
 * Also handles login cookie generation, token transformation, and response mapping.
 */
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

    /**
     * Constructs the proxy service and injects backend service URLs and cookie configurations.
     *
     * @param authServerUrl     base URL for the auth service
     * @param productServiceUrl base URL for the product service
     * @param orderServiceUrl   base URL for the order service
     * @param paymentServiceUrl base URL for the payment service
     * @param cookieSecure      whether the login cookie requires HTTPS
     * @param cookieSameSite    SameSite policy attribute for the login cookie
     * @param httpClient        the HTTP client bean used for proxying requests
     */
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

    /**
     * Forwards an order creation request to the order service.
     *
     * @param requestBody   the JSON payload containing order data
     * @param authorization the Bearer token authorization header, or null
     * @param contentType   the content type of the request
     * @return response entity containing the result from the order service
     */
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

    /**
     * Forwards a payment checkout creation request to the payment service.
     *
     * @param requestBody   the JSON payload containing checkout data
     * @param authorization the Bearer token authorization header, or null
     * @param contentType   the content type of the request
     * @return response entity containing the result from the payment service
     */
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

    /**
     * Retrieves all products from the product service.
     *
     * @param authorization the Bearer token authorization header, or null
     * @return response entity containing the product list JSON
     */
    public ResponseEntity<String> getAllProducts(String authorization) {
        return forwardGet(productServiceUrl, "/api/products", authorization);
    }

    /**
     * Retrieves a specific product by its ID from the product service.
     *
     * @param id            the product identifier
     * @param authorization the Bearer token authorization header, or null
     * @return response entity containing the product details JSON
     */
    public ResponseEntity<String> getProductById(Long id, String authorization) {
        return forwardGet(productServiceUrl, "/api/products/" + id, authorization);
    }

    /**
     * Forwards a request to add a new product to the product service.
     *
     * @param requestBody   the JSON payload containing product details
     * @param authorization the Bearer token authorization header, or null
     * @param contentType   the content type of the request
     * @return response entity containing the result from the product service
     */
    public ResponseEntity<String> addProduct(String requestBody, String authorization, String contentType) {
        return forwardPost(productServiceUrl, "/api/products/add", requestBody, authorization, contentType);
    }

    /**
     * Forwards a request to delete a product by its ID to the product service.
     *
     * @param id            the product identifier
     * @param authorization the Bearer token authorization header, or null
     * @return response entity containing the deletion result
     */
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

    /**
     * Authenticates user credentials against the auth service, sets an HttpOnly login cookie,
     * and strips the raw JWT from the response body returned to the frontend.
     *
     * @param requestBody the JSON payload containing username/credentials
     * @param contentType the content type of the request
     * @return response entity containing user metadata (expiresIn, subject, roles) and a Set-Cookie header
     */
    public ResponseEntity<?> loginWithCookie(String requestBody, String contentType) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(authServerUrl + "/auth/login"))
                .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        return sendLogin(request);
    }

    /**
     * Forwards user registration requests to the auth service.
     *
     * @param requestBody the JSON payload containing registration data
     * @param contentType the content type of the request
     * @return response entity containing the result from the auth service
     */
    public ResponseEntity<String> register(String requestBody, String contentType) {
        return forwardPost(authServerUrl, "/auth/register", requestBody, null, contentType);
    }

    /**
     * Retrieves the JSON Web Key Set (JWKS) from the auth service.
     *
     * @return response entity containing the public keys JSON
     */
    public ResponseEntity<String> getJwks() {
        return forwardGet(authServerUrl, "/auth/jwks", null);
    }

    /**
     * Fetches details of the currently authenticated user from the auth service.
     *
     * @param authorization the Bearer token authorization header derived from the cookie
     * @return response entity containing user details
     */
    public ResponseEntity<String> getMe(String authorization) {
        return forwardGet(authServerUrl, "/auth/me", authorization);
    }

    /**
     * Generates a response entity that clears the login cookie by setting its max age to zero.
     *
     * @return response entity with status 204 No Content and a cookie-clearing Set-Cookie header
     */
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
