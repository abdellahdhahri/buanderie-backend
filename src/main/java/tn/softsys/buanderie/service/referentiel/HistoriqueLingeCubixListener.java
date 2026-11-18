package tn.softsys.buanderie.service.referentiel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import tn.softsys.buanderie.entity.HistoriqueLinge;

/**
 * Écouteur JPA : chaque ligne d'historique d'un linge peut déclencher le report de l'étape dans CUBIX.
 *
 * Avec une EntityManagerFactory déclarée à la main (PostgresConfig), Hibernate crée l'écouteur lui-même, sans
 * injection Spring : le service est donc conservé dans un champ statique, renseigné par l'instance gérée par Spring.
 */
@Component
public class HistoriqueLingeCubixListener {

    private static final Logger log = LoggerFactory.getLogger(HistoriqueLingeCubixListener.class);

    private static volatile CubixEtapeSyncService sync;

    @Autowired
    public void setSync(@Lazy CubixEtapeSyncService service) {
        HistoriqueLingeCubixListener.sync = service;
    }

    @PostPersist
    public void apresCreation(HistoriqueLinge historique) {
        CubixEtapeSyncService service = sync;
        if (service == null) {
            log.warn("Report CUBIX ignoré : service de synchronisation indisponible.");
            return;
        }
        service.planifier(historique);
    }
}
