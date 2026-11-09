package tn.softsys.buanderie.service.Process;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.dto.PliageRequest;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.service.management.ParcoursService;
import java.util.LinkedHashSet;
import java.util.Set;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.AgentRepository;
import tn.softsys.buanderie.repository.HistoriqueLingeRepository;
import tn.softsys.buanderie.repository.LingeRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor

@Transactional
public class PliageService {

    private final LingeRepository lingeRepository;
    private final AgentRepository agentRepository;
    private final HistoriqueLingeRepository historiqueRepository;
    private final PersonnelRepository sqlPersonnelRepo;
    private final ParcoursService parcoursService;

    @Transactional
    public Linge plier(PliageRequest request) {
        parcoursService.exigerActive(CodeEtape.PLIAGE);


        Personnel agent = sqlPersonnelRepo.findByCodeBarre(request.getMatriculeAgent());
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Agent",
//                                "matricule",
//                                request.getMatriculeAgent()));

        Linge linge = rechercherLinge(request.getCodeOuTag());

        Set<EtatLinge> etatsAcceptes = new LinkedHashSet<>(parcoursService.etatsAcceptes(CodeEtape.PLIAGE));
        if (etatsAcceptes.isEmpty()) {
            etatsAcceptes.add(EtatLinge.SECHE);
        }
        if (!etatsAcceptes.contains(linge.getEtat())) {
            throw new BusinessException(
                    "Le linge doit être dans l'un des états suivants : "
                            + String.join(", ", etatsAcceptes.stream().map(Enum::name).toList())
                            + " (état actuel : " + linge.getEtat() + ").");
        }

        EtatLinge ancienEtat = linge.getEtat();

        linge.setEtat(EtatLinge.PLIE);

        linge = lingeRepository.save(linge);

        creerHistorique(
                linge,
                agent.getId(),
                ancienEtat,
                EtatLinge.PLIE,
                "Pliage"
        );

        return linge;
    }

    private Linge rechercherLinge(String codeOuTag) {

        Optional<Linge> linge = lingeRepository.findByCodeBarre(codeOuTag);

        if (linge.isPresent()) {
            return linge.get();
        }
        Linge lingeRFID = lingeRepository.findByRfidTag(codeOuTag);

        if (lingeRFID != null) {
            return lingeRFID;
        }

        throw new ResourceNotFoundException(
                "Linge",
                "codeBarre/rfidTag",
                codeOuTag);
    }

    private void creerHistorique(
            Linge linge,
            Long personnelId ,
            EtatLinge avant,
            EtatLinge apres,
            String observation) {

        historiqueRepository.save(

                HistoriqueLinge.builder()
                        .linge(linge)
                        .personnelId(personnelId)
                        .etatAvant(avant)
                        .etatApres(apres)
                        .typeOperation(TypeOperation.PLIAGE)
                        .dateOperation(LocalDateTime.now())
                        .observations(observation)
                        .build()

        );
    }

}