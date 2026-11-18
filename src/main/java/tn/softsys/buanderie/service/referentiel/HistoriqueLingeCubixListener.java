package tn.softsys.buanderie.service.referentiel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import tn.softsys.buanderie.entity.HistoriqueLinge;

/** Écouteur JPA : chaque ligne d'historique d'un linge peut déclencher le report de l'étape dans CUBIX. */
@Component
public class HistoriqueLingeCubixListener {

    @Autowired
    @Lazy
    private CubixEtapeSyncService sync;

    @PostPersist
    public void apresCreation(HistoriqueLinge historique) {
        if (sync != null) {
            sync.planifier(historique);
        }
    }
}
