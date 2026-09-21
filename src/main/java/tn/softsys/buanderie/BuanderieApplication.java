package tn.softsys.buanderie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching

public class BuanderieApplication {
    public static void main(String[] args) {
        SpringApplication.run(BuanderieApplication.class, args);
    }
}
