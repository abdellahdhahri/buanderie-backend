package tn.softsys.buanderie.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
public class DatabaseCleanUpConfig {

    @Bean
    public CommandLineRunner dropOutdatedConstraints(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                System.out.println("Cleaning up outdated database constraints...");
                
                // PostgreSQL logic to drop constraints if they exist
                List<String> queries = List.of(
                    "ALTER TABLE historique_linge DROP CONSTRAINT IF EXISTS historique_linge_etat_check",
                    "ALTER TABLE linge DROP CONSTRAINT IF EXISTS linge_etat_check"
                );

                for (String sql : queries) {
                    try {
                        jdbcTemplate.execute(sql);
                        System.out.println("Executed: " + sql);
                    } catch (Exception e) {
                        System.err.println("Could not execute [" + sql + "]: " + e.getMessage());
                    }
                }
                
                System.out.println("Database cleanup finished.");
            } catch (Exception e) {
                System.err.println("Error during database cleanup: " + e.getMessage());
            }
        };
    }
}
