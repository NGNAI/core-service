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

@Configuration
@EnableCaching
public class RedisCacheConfig {
	
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
    RedisCacheManager cacheManager(RedisConnectionFactory factory) {
		//Xóa dữ liệu cache cũ khi khởi động ứng dụng
		factory.getConnection().serverCommands().flushDb(); 
		System.out.println("Redis cache cleared on startup.");
		
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
        
//        cacheConfigs.put(
//            "publicMedia",
//            defaultConfig.entryTtl(Duration.ofHours(24))
//        );
//
//        cacheConfigs.put(
//            "articleFeaturedImageMapping",
//            defaultConfig.entryTtl(Duration.ofMinutes(24))
//        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
