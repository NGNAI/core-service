package ai.configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import ai.AppProperties;
import ai.constant.CacheName;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    /** TTL mặc định cho cache gợi ý câu hỏi (phút). */
    private static final long DEFAULT_SUGGESTION_CACHE_TTL_MINUTES = 20L;
	
    @Bean
    @Primary
    RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    	// Tạo một RedisTemplate
        // Với Key là Object
        // Value là Object
        // RedisTemplate giúp chúng ta thao tác với Redis
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // QUAN TRỌNG: Cấu hình Serializer cho Key là String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Với Value, nếu bạn dùng để tăng số (increment), nên dùng String hoặc GenericJackson
        template.setValueSerializer(new StringRedisSerializer()); 
        template.setHashValueSerializer(new JdkSerializationRedisSerializer());

        template.afterPropertiesSet();
        
        return template;
    }
    
    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory factory, AppProperties appProperties) {
		// LƯU Ý: KHÔNG flushDb() khi khởi động — nếu Redis dùng chung cho nhiều ứng dụng
		// sẽ xóa sạch cache của app khác. TTL (48h) là đủ để hết giá trị stale.

		// QUAN TRỌNG: Cấu hình Serializer cho Key là String
        RedisCacheConfiguration defaultConfig =
            RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(new JdkSerializationRedisSerializer())
                )
                .entryTtl(Duration.ofHours(48)); // default TTL

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Cache gợi ý câu hỏi (ask-autocomplete): TTL ngắn vì gợi ý được sinh theo
        // prefix đang gõ — nội dung nhanh lỗi thời, và mục tiêu chính là chặn spam
        // model khi người dùng gõ liên tục. Đọc từ config suggestion.cache-ttl-minutes.
        cacheConfigs.put(
            CacheName.AI_SUGGESTION,
            defaultConfig.entryTtl(suggestionCacheTtl(appProperties))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /** TTL cache gợi ý (mặc định 20 phút) — giá trị <= 0 dùng mặc định. */
    private Duration suggestionCacheTtl(AppProperties appProperties) {
        AppProperties.Suggestion suggestion = appProperties.getSuggestion();
        Long configuredMinutes = suggestion != null ? suggestion.getCacheTtlMinutes() : null;
        long minutes = (configuredMinutes != null && configuredMinutes > 0)
                ? configuredMinutes
                : DEFAULT_SUGGESTION_CACHE_TTL_MINUTES;
        return Duration.ofMinutes(minutes);
    }
}
