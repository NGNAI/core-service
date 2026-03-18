package ai.configuration;

import ai.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ApiClientConfig {
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
}
