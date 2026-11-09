package tn.softsys.buanderie.service.referentiel;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.repository.MachineRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialisationMachines {

    private final MachineRepository machineRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialiser() {
        try {
            machineRepository.migrerMachinesImportees(SourceDonnee.EXTERNE);
            machineRepository.definirSourceManquante(SourceDonnee.INTERNE);
        } catch (Exception e) {
            log.error("Initialisation de la source des machines échouée : {}", e.getMessage());
        }
    }
}
