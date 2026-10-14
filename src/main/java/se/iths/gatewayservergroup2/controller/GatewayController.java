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
    public ResponseEntity<String> createOrder(@RequestBody String requestBody, HttpServletRequest request) {
        return proxyService.createOrder(
                requestBody,
                request.getHeader(HttpHeaders.AUTHORIZATION),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    @PostMapping("/payment/checkout")
    public ResponseEntity<String> createPaymentCheckout(
            @RequestBody String requestBody,
            HttpServletRequest request
    ) {
        return proxyService.createPaymentCheckout(
                requestBody,
                request.getHeader(HttpHeaders.AUTHORIZATION),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    // =============== Product Service Endpoints ==============

    @GetMapping("/products")
    public ResponseEntity<String> getAllProducts(HttpServletRequest request) {
        return proxyService.getAllProducts(
                request.getHeader(HttpHeaders.AUTHORIZATION)
        );
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<String> getProductById(@PathVariable Long id, HttpServletRequest request) {
        return proxyService.getProductById(
                id,
                request.getHeader(HttpHeaders.AUTHORIZATION)
        );
    }

    @PostMapping("/products/add")
    public ResponseEntity<String> addProduct(@RequestBody String requestBody, HttpServletRequest request) {
        return proxyService.addProduct(
                requestBody,
                request.getHeader(HttpHeaders.AUTHORIZATION),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    @DeleteMapping("/products/delete/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        return proxyService.deleteProduct(
                id,
                request.getHeader(HttpHeaders.AUTHORIZATION)
        );
    }
}
