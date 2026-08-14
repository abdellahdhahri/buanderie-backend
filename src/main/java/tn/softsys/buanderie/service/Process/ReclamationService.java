package tn.softsys.buanderie.service.Process;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.dto.ReclamationRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeOperation;
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
                        new ResourceNotFoundException("Agent",
                                "matricule",
                                request.getMatriculeAgent()));
        Linge linge = lingeRepository.findByCodeBarre(request.getCodeOuTag())
                .orElse(null);

        if (linge == null) {
            linge = lingeRepository.findByRfidTag(request.getCodeOuTag());
        }
        if (linge == null) {
            throw new ResourceNotFoundException(
                    "Linge",
                    "code/rfid",
                    request.getCodeOuTag());
        }

        EtatLinge ancienEtat = linge.getEtat();

        Reclamation reclamation = Reclamation.builder()
                .linge(linge)
                .agent(agent)
                .type(request.getType())
                .dateCreation(LocalDateTime.now())
                .status(Reclamation.StatutReclamation.EN_COURS)
                //.resolue(false)
                .build();
        reclamationRepository.save(reclamation);
        //Linge met en hors service
        // Le linge est retiré du circuit
        linge.setEtat(EtatLinge.HORS_SERVICE);
        lingeRepository.save(linge);
        return reclamation;
    }
    public List<Reclamation> getAll(){
        return reclamationRepository.findAll();
    }



}

