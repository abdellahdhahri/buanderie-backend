package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.entity.enums.GroupeLavage;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.AffectationBacRequest;
import tn.softsys.buanderie.dto.BacDTO;
import tn.softsys.buanderie.entity.Bac;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.entity.enums.UsageBac;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.BacRepository;
import tn.softsys.buanderie.repository.HistoriqueLingeRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.service.management.ParcoursService;

@Service
@RequiredArgsConstructor
@Transactional
public class TriService {

    private final LingeRepository lingeRepository;
    private final BacRepository bacRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final PersonnelRepository personnelRepository;
    private final ParcoursService parcoursService;

    @Transactional
    public BacDTO affecterAuBac(AffectationBacRequest request) {
        parcoursService.exigerActive(CodeEtape.TRI);

        Personnel agent = request.getMatriculeAgent() == null
                ? null
                : personnelRepository.findByCodeBarre(request.getMatriculeAgent());
        if (agent == null) {
            throw new BusinessException("Agent introuvable : " + request.getMatriculeAgent());
        }

        Linge linge = trouverLinge(request.getCodeOuTag());
        if (linge == null) {
            throw new ResourceNotFoundException("Linge", "codeBarre/rfidTag", request.getCodeOuTag());
        }

        Set<EtatLinge> etatsAcceptes = etatsAcceptesTri();
        if (!etatsAcceptes.contains(linge.getEtat())) {
            throw new BusinessException("Le linge doit être dans l'un des états suivants : "
                    + String.join(", ", etatsAcceptes.stream().map(Enum::name).toList())
                    + " (état actuel : " + linge.getEtat() + ").");
        }

        Bac bac = trouverBac(request.getCodeBac());
        exigerBacDeTri(bac);

        if (bac.getEtat() != EtatBac.VIDE && bac.getEtat() != EtatBac.EN_TRI) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » n'est pas ouvert au tri (état : " + bac.getEtat() + ").");
        }

        GroupeLavage groupeLinge = groupeDuLinge(linge);
        if (bac.getEtat() == EtatBac.VIDE) {
            bac.setGroupeLavage(groupeLinge);
        } else {
            GroupeLavage groupeBac = groupeDuBac(bac);
            if (groupeBac != groupeLinge) {
                throw new BusinessException("Ce bac contient du linge " + groupeBac
                        + " : ce linge est " + groupeLinge + ". Utilisez un autre bac.");
            }
            bac.setGroupeLavage(groupeBac);
        }
        if (bac.getTypeCollecte() != linge.getTypeCollecte()) {
            throw new BusinessException("Le type de collecte ne correspond pas.");
        }

        double poidsLinge = linge.getPoids() == null ? 0 : linge.getPoids();
        EtatLinge etatAvant = linge.getEtat();

        linge.setBac(bac);
        linge.setEtat(EtatLinge.EN_TRI);
        lingeRepository.save(linge);

        bac.setPoids((bac.getPoids() == null ? 0 : bac.getPoids()) + poidsLinge);
        bac.setEtat(EtatBac.EN_TRI);
        bacRepository.save(bac);

        historiqueLingeRepository.save(
                HistoriqueLinge.builder()
                        .linge(linge)
                        .etatAvant(etatAvant)
                        .etatApres(EtatLinge.EN_TRI)
                        .personnelId(agent.getId())
                        .typeOperation(TypeOperation.TRI)
                        .dateOperation(LocalDateTime.now())
                        .observations("Affectation au bac de tri " + bac.getCode())
                        .build());

        return versDto(bac);
    }

    @Transactional(readOnly = true)
    public List<BacDTO> suggererBacs(String codeOuTag) {
        Linge linge = trouverLinge(codeOuTag);
        if (linge == null) {
            throw new ResourceNotFoundException("Linge", "codeBarre/rfidTag", codeOuTag);
        }
        if (!etatsAcceptesTri().contains(linge.getEtat())) {
            throw new BusinessException(
                    "Le linge n'est pas prêt pour le tri (état actuel : " + linge.getEtat() + ").");
        }

        GroupeLavage groupeLinge = groupeDuLinge(linge);
        return bacRepository.findByUsage(UsageBac.TRI).stream()
                .filter(b -> b.getTypeCollecte() == linge.getTypeCollecte())
                .filter(b -> b.getEtat() == EtatBac.VIDE
                        || (b.getEtat() == EtatBac.EN_TRI && groupeDuBac(b) == groupeLinge))
                .sorted(Comparator
                        .comparing((Bac b) -> b.getEtat() == EtatBac.VIDE ? 1 : 0)
                        .thenComparing(b -> b.getPoids() == null ? 0.0 : b.getPoids()))
                .map(this::versDto)
                .toList();
    }

    private GroupeLavage groupeDuLinge(Linge linge) {
        return linge.getGroupeLavage() != null
                ? linge.getGroupeLavage()
                : GroupeLavage.depuisDesignation(linge.getDesignation());
    }

    /** Groupe du bac : celui enregistré, sinon celui de ses linges, sinon BLANC. */
    private GroupeLavage groupeDuBac(Bac bac) {
        if (bac.getGroupeLavage() != null) return bac.getGroupeLavage();
        return lingeRepository.findByBacId(bac.getId()).stream()
                .findFirst().map(this::groupeDuLinge).orElse(GroupeLavage.BLANC);
    }

    private Set<EtatLinge> etatsAcceptesTri() {
        Set<EtatLinge> etats = new LinkedHashSet<>(parcoursService.etatsAcceptes(CodeEtape.TRI));
        if (etats.isEmpty()) {
            etats.add(EtatLinge.RECEPTION);
        }
        return etats;
    }

    private Linge trouverLinge(String codeOuTag) {
        if (codeOuTag == null || codeOuTag.isBlank()) {
            return null;
        }
        String code = codeOuTag.trim();
        Linge linge = lingeRepository.findByCodeBarre(code).orElse(null);
        return linge != null ? linge : lingeRepository.findByRfidTag(code);
    }

    private Bac trouverBac(String codeBac) {
        if (codeBac == null || codeBac.isBlank()) {
            throw new BusinessException("Le code du bac est obligatoire.");
        }
        String code = codeBac.trim();
        return bacRepository.findByCode(code)
                .or(() -> bacRepository.findByRfidTag(code))
                .orElseThrow(() -> new ResourceNotFoundException("Bac", "code", code));
    }

    private void exigerBacDeTri(Bac bac) {
        if (bac.getUsage() != UsageBac.TRI) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » n'est pas un bac de tri (usage : " + bac.getUsage() + ").");
        }
    }

    private BacDTO versDto(Bac bac) {
        BacDTO dto = BacDTO.from(bac);
        dto.setNombreLinges(lingeRepository.findByBacId(bac.getId()).size());
        return dto;
    }
}
