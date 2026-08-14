package tn.softsys.buanderie.service.Process;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.dto.PliageRequest;
import tn.softsys.buanderie.entity.Agent;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
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

    @Transactional
    public Linge plier(PliageRequest request) {

        Agent agent = agentRepository.findByMatricule(request.getMatriculeAgent())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agent",
                                "matricule",
                                request.getMatriculeAgent()));

        Linge linge = rechercherLinge(request.getCodeOuTag());

        if (linge.getEtat() != EtatLinge.LAVE) {
            throw new BusinessException(
                    "Le linge doit être en état SECHE.");
        }

        EtatLinge ancienEtat = linge.getEtat();

        linge.setEtat(EtatLinge.PLIE);

        linge = lingeRepository.save(linge);

        creerHistorique(
                linge,
                agent,
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
            Agent agent,
            EtatLinge avant,
            EtatLinge apres,
            String observation) {

        historiqueRepository.save(

                HistoriqueLinge.builder()
                        .linge(linge)
                        .agent(agent)
                        .etatAvant(avant)
                        .etatApres(apres)
                        .typeOperation(TypeOperation.PLIAGE)
                        .dateOperation(LocalDateTime.now())
                        .observations(observation)
                        .build()

        );
    }

}