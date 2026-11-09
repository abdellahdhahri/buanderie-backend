package tn.softsys.buanderie.service.referentiel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.ChambreDto;
import tn.softsys.buanderie.dto.LitDto;
import tn.softsys.buanderie.dto.LitInfecteDto;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.Lit;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.LitRepository;

@Service
@RequiredArgsConstructor
public class ChambreLitReferentielService {

    private final ChambreRepository chambreRepository;
    private final LitRepository litRepository;

    @Transactional(readOnly = true)
    public List<ChambreDto> getChambres() {
        return chambreRepository.findActives().stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public ChambreDto getChambreByCodeBarre(String codeBarre) {
        if (codeBarre == null || codeBarre.isBlank()) {
            return null;
        }
        return chambreRepository.findByCodeBarre(codeBarre.trim())
                .filter(c -> !Boolean.FALSE.equals(c.getActif()))
                .map(this::versDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<ChambreDto> findChambreById(Long cle) {
        if (cle == null) {
            return Optional.empty();
        }
        return trouverChambre(cle).map(this::versDto);
    }

    @Transactional(readOnly = true)
    public List<LitDto> getLitsByChambre(Long cleChambre) {
        if (cleChambre == null) {
            return List.of();
        }
        Optional<Chambre> chambre = trouverChambre(cleChambre);
        if (chambre.isEmpty()) {
            return List.of();
        }
        return litRepository.findActifsByChambre(chambre.get().getId()).stream()
                .map(this::versDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LitDto getLitById(Long cle) {
        if (cle == null) {
            return null;
        }
        return trouverLit(cle).map(this::versDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public LitDto getLitByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Lit lit = litRepository.findFirstByCodeOrderByIdAsc(code.trim());
        return lit == null || Boolean.FALSE.equals(lit.getActif()) ? null : versDto(lit);
    }

    @Transactional(readOnly = true)
    public List<LitInfecteDto> getLitsInfectes() {
        return litRepository.findInfectesActifs().stream()
                .map(lit -> LitInfecteDto.builder()
                        .id(CleReferentiel.cle(lit))
                        .code(lit.getCode())
                        .chambreId(CleReferentiel.cle(lit.getChambre()))
                        .estInfecte(true)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> getIdsLitsInfectes() {
        return getLitsInfectes().stream()
                .map(LitInfecteDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Optional<Chambre> trouverChambre(Long cle) {
        Optional<Chambre> externe = chambreRepository.findBySourceAndIdExterne(SourceDonnee.EXTERNE, String.valueOf(cle));
        if (externe.isPresent()) {
            return externe;
        }
        return chambreRepository.findById(cle).filter(c -> Objects.equals(CleReferentiel.cle(c), cle));
    }

    private Optional<Lit> trouverLit(Long cle) {
        Optional<Lit> externe = litRepository.findBySourceAndIdExterne(SourceDonnee.EXTERNE, String.valueOf(cle));
        if (externe.isPresent()) {
            return externe;
        }
        return litRepository.findById(cle).filter(l -> Objects.equals(CleReferentiel.cle(l), cle));
    }

    private ChambreDto versDto(Chambre chambre) {
        ChambreDto dto = new ChambreDto();
        dto.setId(CleReferentiel.cle(chambre));
        dto.setCode(chambre.getCode());
        dto.setServiceId(chambre.getServiceId());
        dto.setCodeBarre(chambre.getCodeBarre());
        return dto;
    }

    private LitDto versDto(Lit lit) {
        return LitDto.builder()
                .id(CleReferentiel.cle(lit))
                .code(lit.getCode())
                .description(lit.getDescription())
                .chambreId(CleReferentiel.cle(lit.getChambre()))
                .infecte(Boolean.TRUE.equals(lit.getInfecte()))
                .build();
    }
}
