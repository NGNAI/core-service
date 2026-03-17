package ai.configuration;

import ai.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient otpRestClient(AppProperties appProperties){
        return RestClient.builder()
                .baseUrl(appProperties.getOtp().getUrl())
                .defaultHeader("X-API-KEY", appProperties.getOtp().getXApiKey())
                .build();
    }
}
