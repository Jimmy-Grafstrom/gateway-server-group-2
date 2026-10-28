package se.iths.gatewayservergroup2.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.gatewayservergroup2.service.GatewayProxyService;

/**
 * Frontend gateway controller handling requests under {@code /gateway}.
 * Delegates proxying to {@link GatewayProxyService} and manages token/cookie conversion.
 */
@RestController
@RequestMapping("/gateway")
public class GatewayController {
    private final GatewayProxyService proxyService;

    public GatewayController(GatewayProxyService proxyService) {
        this.proxyService = proxyService;
    }

    /**
     * Forwards an order creation request to order-service.
     */
    @PostMapping("/order")
    public ResponseEntity<String> createOrder(
            @RequestBody String requestBody,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request) {
        return proxyService.createOrder(
                requestBody,
                toBearerHeader(accessToken),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    /**
     * Forwards a payment checkout request to payment-service.
     */
    @PostMapping("/payment/checkout")
    public ResponseEntity<String> createPaymentCheckout(
            @RequestBody String requestBody,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request
    ) {
        return proxyService.createPaymentCheckout(
                requestBody,
                toBearerHeader(accessToken),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    // =============== Product Service Endpoints ==============

    /**
     * Retrieves all products from product-service.
     */
    @GetMapping("/products")
    public ResponseEntity<String> getAllProducts(
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request) {
        return proxyService.getAllProducts(toBearerHeader(accessToken)
        );
    }

    /**
     * Retrieves a specific product by its ID.
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<String> getProductById(
            @PathVariable Long id,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request) {
        return proxyService.getProductById(id, toBearerHeader(accessToken));
    }

    /**
     * Forwards a product creation request to product-service.
     */
    @PostMapping("/products/add")
    public ResponseEntity<String> addProduct(
            @RequestBody String requestBody,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request) {
        return proxyService.addProduct(
                requestBody,
                toBearerHeader(accessToken),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    /**
     * Forwards a product deletion request to product-service.
     */
    @DeleteMapping("/products/delete/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request) {
        return proxyService.deleteProduct(id, toBearerHeader(accessToken)
        );
    }

    //==================================AUTH-service endpoints============================================

    /**
     * Authenticates user against auth-service and stores JWT in an httpOnly cookie.
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody String requestBody, HttpServletRequest request) {
        return proxyService.loginWithCookie(requestBody, request.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    /**
     * Forwards user registration request to auth-service.
     */
    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@RequestBody String requestBody, HttpServletRequest request) {
        return proxyService.register(requestBody, request.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    /**
     * Retrieves the JWKS public key set from auth-service.
     */
    @GetMapping("/auth/jwks")
    public ResponseEntity<String> getJwks() {
        return proxyService.getJwks();
    }

    /**
     * Logs out user by clearing the access token cookie.
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return proxyService.logout();
    }

    /**
     * Fetches current authenticated user details from auth-service.
     */
    @GetMapping("/auth/me")
    public ResponseEntity<String> me(
            @CookieValue(name = "access_token", required = false) String accessToken) {

        String authorization = toBearerHeader(accessToken);
        if (authorization == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return proxyService.getMe(authorization);
    }

    private String toBearerHeader(String accessToken) {
        return (accessToken != null && !accessToken.isBlank()) ? "Bearer " + accessToken : null;
    }
}
