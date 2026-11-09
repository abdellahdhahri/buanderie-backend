package tn.softsys.buanderie.service.referentiel;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.PersonnelDto;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.service.management.PersonnelService;

@Service
@RequiredArgsConstructor
public class PersonnelReferentielService {

    private final PersonnelRepository personnelRepository;
    private final PersonnelService personnelService;

    public List<PersonnelDto> getAll() {
        return personnelRepository.findByActifTrueOrderByNomAscPrenomAsc()
                .stream()
                .map(personnelService::versDto)
                .toList();
    }

    public PersonnelDto getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant du personnel est obligatoire.");
        }
        Personnel personnel = personnelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personnel introuvable avec l'ID : " + id));
        return personnelService.versDto(personnel);
    }

    public PersonnelDto getByCode(String codeBarre) {
        if (codeBarre == null || codeBarre.isBlank()) {
            throw new IllegalArgumentException("Le code-barres du personnel est obligatoire.");
        }
        Personnel personnel = personnelRepository.findByCodeBarre(codeBarre.trim());
        if (personnel == null) {
            throw new RuntimeException("Personnel introuvable : " + codeBarre.trim());
        }
        return personnelService.versDto(personnel);
    }
}
