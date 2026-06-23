package ai.configuration;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class FlywayConfig {

    public FlywayConfig(DataSource dataSource,
        @Value("${spring.migration.enabled}") boolean enabled,
        @Value("${spring.migration.locations}") String location,
        @Value("${spring.migration.baseline-on-migrate}") boolean baseline
    ) {
        // Khởi tạo và ép cấu hình chạy ngay khi Spring load Context
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location) // Đảm bảo đúng đường dẫn
                .baselineOnMigrate(baseline)             // Tạo bảng history nếu DB đã có sẵn bảng khác
                .load();
        
        // Thêm dòng log này để kiểm tra xem Flyway nhận diện được bao nhiêu file SQL
        log.info("-> Flyway configuration initialized:");
        log.info("- Found Flyway migrations: {}", flyway.info().all().length);
        log.info("- Flyway enabled: {}", enabled);
        log.info("- Flyway locations: {}", location);
        log.info("- Flyway baseline on migrate: {}", baseline);
        if (enabled) {
            log.info("-> Starting Flyway migration...");
            flyway.migrate(); // Kích hoạt chạy thực tế
        }
    }
}