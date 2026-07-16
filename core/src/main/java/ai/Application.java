package ai;

import java.time.LocalDateTime;
import java.util.TimeZone;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import jakarta.annotation.PostConstruct;

@EnableAsync
@SpringBootApplication
public class Application implements CommandLineRunner {
    static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        for (String arg : args) {
			System.out.println("=> "+arg);
		}
		System.out.println("Current JVM version - " + System.getProperty("java.version"));
		System.out.println("Timezone: "+TimeZone.getDefault().getDisplayName());
		System.out.println("System time: " + LocalDateTime.now().toString());
    }
}
