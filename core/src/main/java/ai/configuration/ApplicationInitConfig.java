package ai.configuration;

import ai.entity.postgres.UserEntity;
import ai.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
public class ApplicationInitConfig {
    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository){
        log.info("Init application...");
        return args -> {
            if(userRepository.findByUserName("admin").isEmpty()){
                UserEntity user = UserEntity.builder()
                        .userName("admin")
                        .password(passwordEncoder.encode("admin"))
                        .fullName("Administrator")
                        .email("admin@tmp.com")
                        .source("local")
                        .build();

                userRepository.save(user);
                log.warn("Admin user has been created with default password: admin, please change it!");
            }
        };
    }
}
