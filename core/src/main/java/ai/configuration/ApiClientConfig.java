package ai.configuration;

import ai.AppProperties;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class ApiClientConfig {
    private static final long DEFAULT_INGESTION_READ_TIMEOUT_MS = 180_000L;
    private static final long DEFAULT_OTP_CONNECT_TIMEOUT_MS = 30_000L;
    private static final long DEFAULT_OTP_READ_TIMEOUT_MS = 180_000L;
    private static final long DEFAULT_RAG_CONNECT_TIMEOUT_MS = 60_000L;
    private static final long DEFAULT_RAG_READ_TIMEOUT_MS = 360_000L;

    @Bean
    RestClient otpRestClient(AppProperties appProperties){
        long connectTimeoutMs = appProperties.getOtp().getConnectTimeoutMs() != null
                ? appProperties.getOtp().getConnectTimeoutMs()
                : DEFAULT_OTP_CONNECT_TIMEOUT_MS;
        long readTimeoutMs = appProperties.getOtp().getReadTimeoutMs() != null
                ? appProperties.getOtp().getReadTimeoutMs()
                : DEFAULT_OTP_READ_TIMEOUT_MS;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(appProperties.getOtp().getUrl())
                .defaultHeader("X-API-KEY", appProperties.getOtp().getXApiKey())
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                .build();
    }

    @Bean
    public WebClient ragWebClient(AppProperties appProperties) {
        long connectTimeoutMs = appProperties.getRag().getConnectTimeoutMs() != null
                ? appProperties.getRag().getConnectTimeoutMs()
                : DEFAULT_RAG_CONNECT_TIMEOUT_MS;
        long readTimeoutMs = appProperties.getRag().getReadTimeoutMs() != null
                ? appProperties.getRag().getReadTimeoutMs()
                : DEFAULT_RAG_READ_TIMEOUT_MS;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return WebClient.builder()
                .baseUrl(appProperties.getRag().getUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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
