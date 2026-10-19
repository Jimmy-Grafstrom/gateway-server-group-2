package se.iths.gatewayservergroup2.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.gatewayservergroup2.service.GatewayProxyService;

@RestController
@RequestMapping("/gateway")
public class GatewayController {
    private final GatewayProxyService proxyService;

    public GatewayController(GatewayProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping("/order")
    public ResponseEntity<String> createOrder(
            @RequestBody String requestBody,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request)
    {
        return proxyService.createOrder(
                requestBody,
                toBearerHeader(accessToken),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

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

    @GetMapping("/products")
    public ResponseEntity<String> getAllProducts(
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request)
    {
        return proxyService.getAllProducts(toBearerHeader(accessToken)
        );
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<String> getProductById(
            @PathVariable Long id,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request)
    {
        return proxyService.getProductById(id, toBearerHeader(accessToken));
    }

    @PostMapping("/products/add")
    public ResponseEntity<String> addProduct(
            @RequestBody String requestBody,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request)
    {
        return proxyService.addProduct(
                requestBody,
                toBearerHeader(accessToken),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    @DeleteMapping("/products/delete/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id,
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletRequest request)
    {
        return proxyService.deleteProduct(id, toBearerHeader(accessToken)
        );
    }

    //==================================AUTH-service endpoints============================================

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody String requestBody, HttpServletRequest request) {
        return proxyService.loginWithCookie(requestBody, request.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@RequestBody String requestBody, HttpServletRequest request) {
        return proxyService.register(requestBody, request.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    @GetMapping("/auth/jwks")
    public ResponseEntity<String> getJwks() {
        return proxyService.getJwks();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return proxyService.logout();
    }

    private String toBearerHeader(String accessToken) {
        return (accessToken != null && !accessToken.isBlank()) ? "Bearer " + accessToken : null;
    }
}
