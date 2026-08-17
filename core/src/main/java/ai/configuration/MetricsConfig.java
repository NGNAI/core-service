package ai.configuration;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kích hoạt các annotation metrics của Micrometer:
 * - {@code @Timed}   → đếm số lần gọi + đo thời gian thực thi (timer, có histogram nếu bật)
 * - {@code @Counted} → chỉ đếm số lần gọi (kèm tag {@code exception} khi thất bại)
 *
 * Ví dụ sử dụng trong service:
 * <pre>
 * &#64;Counted(value = "api.topic.list.calls", description = "Số lần gọi get list Topic")
 * &#64;Timed(value = "api.topic.list", description = "Thời gian get list Topic")
 * public CustomPairModel&lt;Long, List&lt;TopicResponseDto&gt;&gt; getAll(TopicFilterDto filterDto) { ... }
 * </pre>
 */
@Configuration
public class MetricsConfig {

    /** Kích hoạt @Timed — timer: <name>_seconds_count / _sum / _max / _bucket (nếu bật histogram) */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /** Kích hoạt @Counted — counter: <name> kèm tag exception (none = thành công) */
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
}
