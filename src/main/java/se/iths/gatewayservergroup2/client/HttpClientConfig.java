package se.iths.gatewayservergroup2.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration class that provides a shared {@link HttpClient} bean with a 5-second connection timeout.
 */
@Configuration
public class HttpClientConfig {

    /**
     * Creates and configures the HTTP client instance used for proxying requests.
     *
     * @return the configured {@link HttpClient}
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
}