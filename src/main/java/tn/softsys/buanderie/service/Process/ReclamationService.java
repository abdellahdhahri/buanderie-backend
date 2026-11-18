package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.repository.MachineRepository;

import tn.softsys.buanderie.entity.Machine;
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
    private final MachineRepository machineRepository;
    private final AgentRepository agentRepository;
    private final ReclamationRepository reclamationRepository;
    private final HistoriqueLingeRepository historiqueRepository;
    private final NotificationService notificationService;
    private final PersonnelRepository personnelRepository;
    private final PersonnelReferentielService personnelReferentielService;



    @Transactional
    public Reclamation creer(ReclamationRequest request) {

        if (request.getCodeMachine() != null && !request.getCodeMachine().isBlank()) {
            return creerPourMachine(request);
        }

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
                .personnelId(agent.getId())
                .personnelNom(agent.getNomPrenom())
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
    /** Réclamation sur une machine : la panne la rend indisponible tant qu'elle n'est pas résolue. */
    private Reclamation creerPourMachine(ReclamationRequest request) {
        if (request.getType() == null || !request.getType().name().startsWith("MACHINE_")) {
            throw new tn.softsys.buanderie.exception.BusinessException(
                    "Type de réclamation machine invalide : " + request.getType());
        }
        Machine machine = machineRepository.findByCode(request.getCodeMachine().trim());
        if (machine == null) {
            throw new ResourceNotFoundException("Machine", "code", request.getCodeMachine());
        }

        PersonnelDto declarant = request.getMatriculeAgent() == null || request.getMatriculeAgent().isBlank()
                ? null : personnelReferentielService.getByCode(request.getMatriculeAgent());

        Reclamation reclamation = Reclamation.builder()
                .machine(machine)
                .personnelId(declarant == null ? null : declarant.getId())
                .personnelNom(declarant == null ? null : declarant.getNomPrenom())
                .type(request.getType())
                .description(request.getDescription() == null || request.getDescription().isBlank()
                        ? null : request.getDescription().trim())
                .dateCreation(LocalDateTime.now())
                .status(Reclamation.StatutReclamation.EN_COURS)
                .build();
        reclamationRepository.save(reclamation);

        machine.setEtatTechnique(request.getType() == TypeReclamation.MACHINE_EN_PANNE
                ? tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.EN_PANNE
                : tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.FONCTIONNEL_AVEC_PROBLEME);
        machineRepository.save(machine);

        notificationService.notifierReclamationMachine(machine, reclamation);
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

        // Machine : la réclamation résolue ou fermée la remet en service
        if (reclamation.getMachine() != null) {
            if (newStatus == Reclamation.StatutReclamation.RESOLUE
                    || newStatus == Reclamation.StatutReclamation.FERMEE) {
                Machine m = reclamation.getMachine();
                boolean autreOuverte = reclamationRepository.findAll().stream()
                        .anyMatch(r -> r.getMachine() != null
                                && r.getMachine().getId().equals(m.getId())
                                && !r.getId().equals(reclamation.getId())
                                && r.getStatus() != Reclamation.StatutReclamation.RESOLUE
                                && r.getStatus() != Reclamation.StatutReclamation.FERMEE);
                if (!autreOuverte) {
                    m.setEtatTechnique(tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.FONCTIONNEL);
                    machineRepository.save(m);
                }
            }
            return reclamationRepository.save(reclamation);
        }

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

