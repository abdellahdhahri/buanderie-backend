package tn.softsys.buanderie.service.referentiel;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.LitRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialisationChambresLits {

    private final ChambreRepository chambreRepository;
    private final LitRepository litRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialiser() {
        try {
            chambreRepository.activerNonRenseignees();
            chambreRepository.migrerChambresImportees(SourceDonnee.EXTERNE);
            litRepository.activerNonRenseignes();
            litRepository.infecteParDefaut();
            litRepository.definirSourceManquante(SourceDonnee.INTERNE);
        } catch (Exception e) {
            log.error("Initialisation des chambres et des lits échouée : {}", e.getMessage());
        }
    }
}
