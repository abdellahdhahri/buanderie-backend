package tn.softsys.buanderie.service.Process;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.dto.ReclamationRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.entity.enums.TypeReclamation;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import java.time.LocalDateTime;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class ReclamationService {
    private final LingeRepository lingeRepository;
    private final AgentRepository agentRepository;
    private final ReclamationRepository reclamationRepository;
    private final HistoriqueLingeRepository historiqueRepository;
    public Reclamation creer(ReclamationRequest request) {

        System.out.println("MAT: " + request.getMatriculeAgent());
        System.out.println("CODE: " + request.getCodeOuTag());

        Agent agent = agentRepository.findByMatricule(request.getMatriculeAgent())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agent",
                                "matricule",
                                request.getMatriculeAgent()
                        )
                );

        Linge linge = lingeRepository.findByCodeBarre(request.getCodeOuTag())
                .orElse(null);

        if (linge == null) {
            linge = lingeRepository.findByRfidTag(request.getCodeOuTag());
        }

        if (linge == null) {
            throw new ResourceNotFoundException(
                    "Linge",
                    "code/rfid",
                    request.getCodeOuTag()
            );
        }

        // État actuel avant modification
        EtatLinge ancienEtat = linge.getEtat();

        Reclamation reclamation = Reclamation.builder()
                .linge(linge)
                .agent(agent)
                .type(request.getType())
                .dateCreation(LocalDateTime.now())
                .status(Reclamation.StatutReclamation.EN_COURS)
              //  .resolue(false)
                .build();

        reclamationRepository.save(reclamation);


        EtatLinge nouvelEtat;
        String observations;

        if (request.getType() == TypeReclamation.TACHE_PERSISTANTE
                || request.getType() == TypeReclamation.MAL_LAVE) {

            // Le linge retourne à la réception
            nouvelEtat = EtatLinge.RECEPTION;

            observations = "Réclamation qualité lavage : "
                    + request.getType().name()
                    + ". Le linge est retourné à l'état RECEPTIONNE pour retraitement.";

        } else {

            // Réclamations nécessitant le retrait du linge du circuit
            nouvelEtat = EtatLinge.HORS_SERVICE;

            observations = "Réclamation : "
                    + request.getType().name()
                    + ". Le linge est retiré du circuit et mis hors service.";
        }

        // Modification de l'état
        linge.setEtat(nouvelEtat);
        lingeRepository.save(linge);


        HistoriqueLinge historique = HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(ancienEtat)
                .etatApres(nouvelEtat)
                .agent(agent)
                .typeOperation(TypeOperation.CHANGEMENT_ETAT_LINGE)
                .dateOperation(LocalDateTime.now())
                .observations(observations)
                .build();

        historiqueRepository.save(historique);

        return reclamation;
    }
    public List<Reclamation> getAll(){
        return reclamationRepository.findAll();
    }



}

