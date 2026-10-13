package tn.softsys.buanderie.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CategLingeConstraintMigration {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void supprimerAnciennesContraintesCategorie() {
        try {
            List<Map<String, Object>> contraintes = jdbcTemplate.queryForList("""
                    SELECT c.conrelid::regclass::text AS table_name, c.conname AS constraint_name
                    FROM pg_constraint c
                    WHERE c.contype = 'c'
                      AND pg_get_constraintdef(c.oid) LIKE '%TAIE_OREILLER%'
                      AND pg_get_constraintdef(c.oid) NOT LIKE '%CHAMP_OPERATOIRE%'
                    """);

            for (Map<String, Object> ligne : contraintes) {
                String table = (String) ligne.get("table_name");
                String contrainte = (String) ligne.get("constraint_name");
                jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS \"" + contrainte + "\"");
                System.out.println("Contrainte categorie supprimée : " + table + "." + contrainte);
            }
        } catch (Exception e) {
            System.err.println("Migration contraintes categorie échouée : " + e.getMessage());
        }
    }
}
