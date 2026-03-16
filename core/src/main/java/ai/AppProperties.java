package ai;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Component
@ConfigurationProperties
public class AppProperties {
    Jwt jwt;
    Otp otp;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Jwt {
        String secretKey;
        long expiryDuration;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Otp {
        String url;
        String xApiKey;
    }
}