package ai.configuration;

import ai.AppProperties;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class ApiClientConfig {
    private static final long DEFAULT_INGESTION_READ_TIMEOUT_MS = 180_000L;

    @Bean
    RestClient otpRestClient(AppProperties appProperties){
        return RestClient.builder()
                .baseUrl(appProperties.getOtp().getUrl())
                .defaultHeader("X-API-KEY", appProperties.getOtp().getXApiKey())
                .build();
    }

    @Bean
    public WebClient ragWebClient(AppProperties appProperties) {
        return WebClient.builder()
                .baseUrl(appProperties.getRag().getUrl())
                .build();
    }

    @Bean
    RestClient ingestionRestClient(AppProperties appProperties) {
    long readTimeoutMs = appProperties.getIngestion().getReadTimeoutMs() != null
        ? appProperties.getIngestion().getReadTimeoutMs()
        : DEFAULT_INGESTION_READ_TIMEOUT_MS;

    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) readTimeoutMs)
        .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(appProperties.getIngestion().getUrl())
        .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                .build();
    }
}
