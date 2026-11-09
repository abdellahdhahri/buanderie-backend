package tn.softsys.buanderie.service.management;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.LingeEtapeDTO;
import tn.softsys.buanderie.entity.LingeEtape;
import tn.softsys.buanderie.entity.LingeEtapeSuivante;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.Zone;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.LingeEtapeRepository;
import tn.softsys.buanderie.repository.LingeEtapeSuivanteRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class LingeEtapeService {

    private final LingeEtapeRepository repository;
    private final LingeEtapeSuivanteRepository suivanteRepository;

    public List<LingeEtapeDTO> getAll() {
        return versDtos(repository.findAllByOrderByOrdreAsc());
    }

    public LingeEtapeDTO getById(Long id) {
        return versDto(charger(id));
    }

    public LingeEtapeDTO getByCodeBarre(String codeBarre) {
        return versDto(repository.findByCodeBarre(codeBarre)
                .orElseThrow(() -> new BusinessException("Étape introuvable avec le code-barres : " + codeBarre)));
    }

    public List<LingeEtapeDTO> getByZone(Zone zone) {
        return versDtos(repository.findByZone(zone));
    }

    public List<LingeEtapeDTO> getByCategorie(String categorie) {
        return versDtos(repository.findByCategorie(categorie));
    }

    public List<LingeEtapeDTO> getByZoneAndCategorie(String zone, String categorie) {
        return versDtos(repository.findByZoneAndCategorie(zone, categorie));
    }

    public LingeEtapeDTO create(LingeEtape lingeEtape) {
        Integer dernierOrdre = repository.findMaxOrdre();
        int nouvelOrdre = (dernierOrdre == null) ? 1 : dernierOrdre + 1;

        lingeEtape.setId(null);
        lingeEtape.setCode(null);
        lingeEtape.setActif(true);
        lingeEtape.setObligatoire(false);
        lingeEtape.setOrdre(nouvelOrdre);
        lingeEtape.setCodeBarre(String.format("E%04d", nouvelOrdre));

        return versDto(repository.save(lingeEtape));
    }

    public LingeEtapeDTO update(Long id, LingeEtape data) {
        LingeEtape existing = charger(id);

        if (data.getCodeBarre() != null && !data.getCodeBarre().isBlank()
                && !data.getCodeBarre().equals(existing.getCodeBarre())) {
            if (repository.existsByCodeBarre(data.getCodeBarre())) {
                throw new BusinessException("Le code-barres existe déjà : " + data.getCodeBarre());
            }
            existing.setCodeBarre(data.getCodeBarre());
        }
        if (data.getNom() != null && !data.getNom().isBlank()) {
            existing.setNom(data.getNom());
        }
        if (data.getCategorie() != null && !data.getCategorie().isBlank()) {
            existing.setCategorie(data.getCategorie());
        }
        if (data.getZone() != null && existing.getCode() == null) {
            existing.setZone(data.getZone());
        }

        return versDto(repository.save(existing));
    }

    public void delete(Long id) {
        LingeEtape existing = charger(id);
        if (existing.getCode() != null) {
            throw new BusinessException(
                    "L'étape « " + existing.getNom() + " » est une étape standard : désactivez-la au lieu de la supprimer.");
        }
        repository.delete(existing);
    }

    private LingeEtape charger(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Étape introuvable avec l'id : " + id));
    }

    private List<LingeEtapeDTO> versDtos(List<LingeEtape> etapes) {
        List<LingeEtapeDTO> resultat = new ArrayList<>();
        for (LingeEtape etape : etapes) {
            resultat.add(versDto(etape));
        }
        return resultat;
    }

    private LingeEtapeDTO versDto(LingeEtape etape) {
        List<CodeEtape> suivantes = new ArrayList<>();
        for (LingeEtapeSuivante lien : suivanteRepository.findByEtape_Id(etape.getId())) {
            CodeEtape code = lien.getEtapeSuivante().getCode();
            if (code != null) {
                suivantes.add(code);
            }
        }
        return LingeEtapeDTO.depuis(etape, suivantes);
    }
}
