package tn.softsys.buanderie.service.referentiel;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.repository.LingeRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialisationLinges {

    private final LingeRepository lingeRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialiser() {
        try {
            lingeRepository.migrerLingesImportes(SourceDonnee.EXTERNE);
            lingeRepository.definirSourceManquante(SourceDonnee.INTERNE);
        } catch (Exception e) {
            log.error("Initialisation de la source des linges échouée : {}", e.getMessage());
        }
    }
}
