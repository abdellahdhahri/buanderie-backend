package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.ProgrammeLavage;
import tn.softsys.buanderie.entity.ProgrammeSechage;
import tn.softsys.buanderie.entity.enums.TypeProgramme;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.ProgrammeLavageRepository;
import tn.softsys.buanderie.repository.ProgrammeSechageRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProgrammeService {

    private final ProgrammeLavageRepository lavageRepo;
    private final ProgrammeSechageRepository sechageRepo;

    // ==================== PROGRAMMES DE LAVAGE ====================

    @Transactional(readOnly = true)
    public List<ProgrammeLavage> findAllLavage() {
        log.debug("Récupération de tous les programmes de lavage");
        return lavageRepo.findAll();
    }

    @Transactional(readOnly = true)
    public ProgrammeLavage findLavageById(Long id) {
        log.debug("Récupération du programme de lavage avec l'ID : {}", id);
        return lavageRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProgrammeLavage", id));
    }

    public ProgrammeLavage creerLavage(ProgrammeLavage programme) {
        log.info("Création d'un nouveau programme de lavage : {}", programme.getNom());
        validateProgrammeLavage(programme);
        if (Boolean.TRUE.equals(programme.getParDefaut())) {
            verifierEligibleParDefaut(programme);
            retirerAutresParDefaut(null);
        }
        return lavageRepo.save(programme);
    }

    public ProgrammeLavage modifierLavage(Long id, ProgrammeLavage data) {
        log.info("Modification du programme de lavage avec l'ID : {}", id);
        ProgrammeLavage existing = findLavageById(id);

        // Mise à jour des champs
        if (data.getNom() != null) {
            existing.setNom(data.getNom());
        }
        if (data.getTemperatureCelsius() != null) {
            existing.setTemperatureCelsius(data.getTemperatureCelsius());
        }
        if (data.getDureeMinutes() != null) {
            existing.setDureeMinutes(data.getDureeMinutes());
        }
        if (data.getVitesseEssorageRpm() != null) {
            existing.setVitesseEssorageRpm(data.getVitesseEssorageRpm());
        }
        if (data.getDescription() != null) {
            existing.setDescription(data.getDescription());
        }
        if (data.getCategories() != null && !data.getCategories().isEmpty()) {
            existing.setCategories(data.getCategories());
        }

        if (data.getParDefaut() != null) {
            existing.setParDefaut(data.getParDefaut());
        }
        if (data.getGroupeLavage() != null) {
            existing.setGroupeLavage(data.getGroupeLavage());
        }

        validateProgrammeLavage(existing);
        if (Boolean.TRUE.equals(existing.getParDefaut())) {
            verifierEligibleParDefaut(existing);
            retirerAutresParDefaut(existing.getId());
        }
        return lavageRepo.save(existing);
    }

    public void supprimerLavage(Long id) {
        log.info("Suppression du programme de lavage avec l'ID : {}", id);
        ProgrammeLavage programme = findLavageById(id);
        lavageRepo.delete(programme);
    }

    // ==================== PROGRAMMES DE SÉCHAGE ====================

    @Transactional(readOnly = true)
    public List<ProgrammeSechage> findAllSechage() {
        log.debug("Récupération de tous les programmes de séchage");
        return sechageRepo.findAll();
    }

    @Transactional(readOnly = true)
    public ProgrammeSechage findSechageById(Long id) {
        log.debug("Récupération du programme de séchage avec l'ID : {}", id);
        return sechageRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProgrammeSechage", id));
    }

    public ProgrammeSechage creerSechage(ProgrammeSechage programme) {
        log.info("Création d'un nouveau programme de séchage : {}", programme.getNom());
        validateProgrammeSechage(programme);
        return sechageRepo.save(programme);
    }

    public ProgrammeSechage modifierSechage(Long id, ProgrammeSechage data) {
        log.info("Modification du programme de séchage avec l'ID : {}", id);
        ProgrammeSechage existing = findSechageById(id);

        // Mise A jour des champs
        if (data.getNom() != null) {
            existing.setNom(data.getNom());
        }
        if (data.getTemperatureCelsius() != null) {
            existing.setTemperatureCelsius(data.getTemperatureCelsius());
        }
        if (data.getDureeMinutes() != null) {
            existing.setDureeMinutes(data.getDureeMinutes());
        }
        if (data.getNiveauHumidite() != null) {
            existing.setNiveauHumidite(data.getNiveauHumidite());
        }
        if (data.getDescription() != null) {
            existing.setDescription(data.getDescription());
        }
        if (data.getCategories() != null && !data.getCategories().isEmpty()) {
            existing.setCategories(data.getCategories());
        }

        validateProgrammeSechage(existing);
        return sechageRepo.save(existing);
    }

    public void supprimerSechage(Long id) {
        log.info("Suppression du programme de séchage avec l'ID : {}", id);
        ProgrammeSechage programme = findSechageById(id);
        sechageRepo.delete(programme);
    }


    private void verifierEligibleParDefaut(ProgrammeLavage programme) {
        if (programme.getType() != TypeProgramme.LAVAGE) {
            throw new BusinessException(
                    "Seul un programme de type LAVAGE peut être défini par défaut.");
        }
    }

    // Un seul programme par défaut : on retire l'indicateur aux autres.
    private void retirerAutresParDefaut(Long idAConserver) {
        for (ProgrammeLavage autre : lavageRepo.findByParDefautTrue()) {
            if (idAConserver == null || !idAConserver.equals(autre.getId())) {
                autre.setParDefaut(false);
                lavageRepo.save(autre);
            }
        }
    }

    private void validateProgrammeLavage(ProgrammeLavage programme) {
        if (programme.getNom() == null || programme.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du programme de lavage est obligatoire");
        }

        if (programme.getTemperatureCelsius() != null) {
            if (programme.getTemperatureCelsius() < 0 || programme.getTemperatureCelsius() > 100) {
                throw new IllegalArgumentException("La température doit être comprise entre 0 et 100°C");
            }
        }

        if (programme.getDureeMinutes() != null) {
            if (programme.getDureeMinutes() <= 0) {
                throw new IllegalArgumentException("La durée doit être positive");
            }
        }

        if (programme.getVitesseEssorageRpm() != null) {
            if (programme.getVitesseEssorageRpm() < 0) {
                throw new IllegalArgumentException("La vitesse d'essorage ne peut pas être négative");
            }
        }
    }

    private void validateProgrammeSechage(ProgrammeSechage programme) {
        if (programme.getNom() == null || programme.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du programme de séchage est obligatoire");
        }

        if (programme.getTemperatureCelsius() != null) {
            if (programme.getTemperatureCelsius() < 0 || programme.getTemperatureCelsius() > 100) {
                throw new IllegalArgumentException("La température doit être comprise entre 0 et 100°C");
            }
        }

        if (programme.getDureeMinutes() != null) {
            if (programme.getDureeMinutes() <= 0) {
                throw new IllegalArgumentException("La durée doit être positive");
            }
        }

        if (programme.getNiveauHumidite() != null) {
            if (programme.getNiveauHumidite() < 0 || programme.getNiveauHumidite() > 100) {
                throw new IllegalArgumentException("Le niveau d'humidité doit être compris entre 0 et 100%");
            }
        }
    }
}