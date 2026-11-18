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

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void verifierSynchronisations() {
        try {
            ConfigurationIntegration config = integrationService.getConfiguration();
            if (config.getMode() != ModeSource.EXTERNE || !Boolean.TRUE.equals(config.getSynchroAutomatique())) {
                return;
            }

            if (config.getTypeAuthentification() == TypeAuthentification.CUBIX) {
                return;
            }
            int frequence = config.getFrequenceMinutes() == null ? 60 : config.getFrequenceMinutes();

            for (EndpointIntegration endpoint : integrationService.getEndpoints()) {
                if (!Boolean.TRUE.equals(endpoint.getActif()) || !endpoint.getReferentiel().isSynchronisationDisponible()) {
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
