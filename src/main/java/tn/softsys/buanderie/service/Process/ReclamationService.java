package tn.softsys.buanderie.service.Process;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.service.referentiel.PersonnelReferentielService;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.dto.PersonnelDto;
import tn.softsys.buanderie.dto.ReclamationRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.entity.enums.TypeReclamation;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.Reporting.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class ReclamationService {
    private final LingeRepository lingeRepository;
    private final AgentRepository agentRepository;
    private final ReclamationRepository reclamationRepository;
    private final HistoriqueLingeRepository historiqueRepository;
    private final NotificationService notificationService;
    private final PersonnelRepository sqlPersonnelRepo;
    private final PersonnelReferentielService personnelReferentielService;



    @Transactional
    public Reclamation creer(ReclamationRequest request) {

        PersonnelDto agent = personnelReferentielService
                .getByCode(request.getMatriculeAgent());
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Agent",
//                                "matricule",
//                                request.getMatriculeAgent()
//                        )
//                );

        Linge linge = lingeRepository
                .findByCodeBarre(request.getCodeOuTag())
                .orElse(null);

        if (linge == null) {
            linge = lingeRepository
                    .findByRfidTag(request.getCodeOuTag());
        }

        if (linge == null) {
            throw new ResourceNotFoundException(
                    "Linge",
                    "code/rfid",
                    request.getCodeOuTag()
            );
        }

        EtatLinge ancienEtat = linge.getEtat();

        Reclamation reclamation = Reclamation.builder()
                .linge(linge)
                //   .agent(agent)
                .type(request.getType())
                .dateCreation(LocalDateTime.now())
                .status(Reclamation.StatutReclamation.EN_COURS)
                .build();

        reclamationRepository.save(reclamation);


        EtatLinge nouvelEtat;
        String observations;

        if (request.getType() == TypeReclamation.TACHE_PERSISTANTE
                || request.getType() == TypeReclamation.MAL_LAVE) {

            nouvelEtat = EtatLinge.RECEPTION;

            observations =
                    "Réclamation qualité lavage : "
                            + request.getType().name()
                            + ". Le linge est retourné à l'état RECEPTION pour retraitement.";

        } else {

            nouvelEtat = EtatLinge.HORS_SERVICE;

            observations =
                    "Réclamation : "
                            + request.getType().name()
                            + ". Le linge est retiré du circuit et mis hors service.";
        }


        linge.setEtat(nouvelEtat);

        lingeRepository.save(linge);


        HistoriqueLinge historique = HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(ancienEtat)
                .etatApres(nouvelEtat)
                .personnelId(agent.getId())
                .typeOperation(TypeOperation.CHANGEMENT_ETAT_LINGE)
                .dateOperation(LocalDateTime.now())
                .observations(observations)
                .build();

        historiqueRepository.save(historique);


        //  Notification
        notificationService.notifierReclamation(
                linge,
                reclamation
        );


        return reclamation;
    }
    public List<Reclamation> getAll(){
        return reclamationRepository.findAll();
    }

    @Transactional
    public Reclamation updateStatus(Long reclamationId, Reclamation.StatutReclamation newStatus) {

        Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        reclamation.setStatus(newStatus);

        // Cas particulier : linge mal lavé
        if (reclamation.getType() == TypeReclamation.MAL_LAVE
                && newStatus == Reclamation.StatutReclamation.RESOLUE) {

            Linge linge = reclamation.getLinge();

            if (linge != null) {
                linge.setEtat(EtatLinge.STOCKE);
                lingeRepository.save(linge);
            }
        }

        return reclamationRepository.save(reclamation);
    }

}

