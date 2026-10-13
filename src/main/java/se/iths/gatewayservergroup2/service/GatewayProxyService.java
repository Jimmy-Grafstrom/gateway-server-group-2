package se.iths.gatewayservergroup2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class GatewayProxyService {
    private final String authServerUrl;
    private final String productServiceUrl;
    private final String orderServiceUrl;
    private final String paymentServiceUrl;
    private final HttpClient httpClient;

    public GatewayProxyService(
            @Value("${app.auth-server-url}") String authServerUrl,
            @Value("${app.product-service-url}") String productServiceUrl,
            @Value("${app.order-service-url}") String orderServiceUrl,
            @Value("${app.payment-service-url}") String paymentServiceUrl,
            HttpClient httpClient) {
        this.authServerUrl = authServerUrl;
        this.productServiceUrl = productServiceUrl;
        this.orderServiceUrl = orderServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
        this.httpClient = httpClient;
    }

    public ResponseEntity<String> createOrder (
            String requestBody,
            String authorization,
            String contentType) {
        return forwardPost (
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
        try{
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
}
