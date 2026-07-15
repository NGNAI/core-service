package ai.logging;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký {@link RequestLoggingInterceptor} chỉ khi profile {@code dev} active.
 * <p>
 * Dùng {@code --spring.profiles.active=dev} (hoặc {@code application-dev.yml})
 * để bật logging chi tiết request/response khi phát triển.
 */
// @Profile("dev")
// @Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<RequestLoggingInterceptor> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingInterceptor> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestLoggingInterceptor());
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE); // chạy đầu tiên trong filter chain
        registration.setName("requestLoggingInterceptor");
        return registration;
    }
}
