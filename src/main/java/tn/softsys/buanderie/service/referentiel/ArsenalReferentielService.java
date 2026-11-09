package tn.softsys.buanderie.service.referentiel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.ArsenalDto;
import tn.softsys.buanderie.dto.EmplacementDto;
import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.Emplacement;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.repository.ArsenalRepository;
import tn.softsys.buanderie.repository.EmplacementRepository;

@Service
@RequiredArgsConstructor
public class ArsenalReferentielService {

    private final ArsenalRepository arsenalRepository;
    private final EmplacementRepository emplacementRepository;

    @Transactional(readOnly = true)
    public List<ArsenalDto> getArsenaux() {
        return arsenalRepository.findActifs().stream()
                .map(this::versDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmplacementDto> getEmplacements(Long cleArsenal) {
        List<Emplacement> emplacements;

        if (cleArsenal == null) {
            emplacements = emplacementRepository.findByActifTrueOrderByArsenal_IdAscNumeroAscNomAsc();
        } else {
            Optional<Arsenal> arsenal = trouverArsenal(cleArsenal)
                    .filter(a -> !Boolean.FALSE.equals(a.getActif()));
            if (arsenal.isEmpty()) {
                return List.of();
            }
            emplacements = emplacementRepository
                    .findByArsenal_IdAndActifTrueOrderByNumeroAscNomAsc(arsenal.get().getId());
        }

        return emplacements.stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmplacementDto> getEmplacementById(Long cleEmplacement) {
        if (cleEmplacement == null) {
            throw new IllegalArgumentException("L'identifiant de l'emplacement est obligatoire");
        }
        return trouverEmplacement(cleEmplacement).map(this::versDto);
    }

    @Transactional(readOnly = true)
    public Optional<EmplacementDto> getEmplacementByCodeBarre(String codeBarre) {
        if (codeBarre == null || codeBarre.isBlank()) {
            throw new IllegalArgumentException("Le code-barres de l'emplacement est obligatoire");
        }
        return emplacementRepository.findByCodeBarre(codeBarre.trim())
                .filter(e -> !Boolean.FALSE.equals(e.getActif()))
                .map(this::versDto);
    }

    private Optional<Arsenal> trouverArsenal(Long cle) {
        Optional<Arsenal> externe = arsenalRepository.findBySourceAndIdExterne(SourceDonnee.EXTERNE, String.valueOf(cle));
        if (externe.isPresent()) {
            return externe;
        }
        return arsenalRepository.findById(cle).filter(a -> Objects.equals(CleReferentiel.cle(a), cle));
    }

    private Optional<Emplacement> trouverEmplacement(Long cle) {
        Optional<Emplacement> externe = emplacementRepository
                .findBySourceAndIdExterne(SourceDonnee.EXTERNE, String.valueOf(cle));
        if (externe.isPresent()) {
            return externe;
        }
        return emplacementRepository.findById(cle).filter(e -> Objects.equals(CleReferentiel.cle(e), cle));
    }

    private ArsenalDto versDto(Arsenal arsenal) {
        return ArsenalDto.builder()
                .arsenalID(CleReferentiel.cle(arsenal))
                .designation(arsenal.getNom())
                .codeBarre(arsenal.getCode())
                .build();
    }

    private EmplacementDto versDto(Emplacement emplacement) {
        EmplacementDto dto = new EmplacementDto();
        dto.setEmplacementID(CleReferentiel.cle(emplacement));
        dto.setArsenalID(CleReferentiel.cle(emplacement.getArsenal()));
        dto.setNom(emplacement.getNom());
        dto.setNumero(emplacement.getNumero());
        dto.setCodeBarre(emplacement.getCodeBarre());
        return dto;
    }
}
