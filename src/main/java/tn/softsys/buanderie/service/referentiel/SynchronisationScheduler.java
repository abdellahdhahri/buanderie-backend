package tn.softsys.buanderie.service.referentiel;

import tn.softsys.buanderie.entity.enums.TypeAuthentification;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.enums.ModeSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class SynchronisationScheduler {

    private final IntegrationService integrationService;
    private final SynchronisationService synchronisationService;
    private final tn.softsys.buanderie.security.CubixTokenStore cubixTokens;

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void verifierSynchronisations() {
        try {
            ConfigurationIntegration config = integrationService.getConfiguration();
            if (config.getMode() != ModeSource.EXTERNE || !Boolean.TRUE.equals(config.getSynchroAutomatique())) {
                return;
            }

            boolean cubix = config.getTypeAuthentification() == TypeAuthentification.CUBIX;
            int frequence = config.getFrequenceMinutes() == null ? 60 : config.getFrequenceMinutes();

            for (EndpointIntegration endpoint : integrationService.getEndpoints()) {
                if (!Boolean.TRUE.equals(endpoint.getActif()) || !endpoint.getReferentiel().isSynchronisationDisponible()) {
                    continue;
                }
                // CUBIX : les référentiels /Importer/ sont ouverts ; ceux de BuanderieApi exigent une session
                if (cubix && integrationService.urlComplete(endpoint.getReferentiel()).contains("/BuanderieApi/")
                        && cubixTokens.jetonCourant() == null) {
                    continue;
                }
                boolean due = endpoint.getDerniereSynchro() == null
                        || endpoint.getDerniereSynchro().plusMinutes(frequence).isBefore(LocalDateTime.now());
                if (due) {
                    try {
                        synchronisationService.synchroniser(endpoint.getReferentiel(), "AUTO");
                    } catch (Exception e) {
                        log.error("Synchronisation automatique de {} échouée : {}", endpoint.getReferentiel(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Vérification des synchronisations échouée : {}", e.getMessage());
        }
    }
}
