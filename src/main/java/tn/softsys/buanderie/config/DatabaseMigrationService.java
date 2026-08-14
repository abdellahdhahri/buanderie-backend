package tn.softsys.buanderie.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    //@PostConstruct
    public void migrate() {
        try {
            jdbcTemplate.update("UPDATE linge SET etat = 'SECHE' WHERE etat = 'SEC'");
            jdbcTemplate.update("UPDATE historique_linge SET etat = 'SECHE' WHERE etat = 'SEC'");
            System.out.println("Migration from SEC to SECHE successful!");
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
        }
    }
}
