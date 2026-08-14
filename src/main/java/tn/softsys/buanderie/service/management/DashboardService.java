package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.dto.DashboardDTO;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeReclamation;
import tn.softsys.buanderie.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final BacRepository bacRepository;
    private final MachineRepository machineRepository;
    private final ReclamationRepository reclamationRepository;
    private final HistoriqueBacRepository historiqueBacRepository;
    private final LingeRepository lingeRepository;
    private final AgentRepository agentRepository;

    public DashboardDTO getDashboard() {
        List<Bac> tousBacs = bacRepository.findAll();
        // Compteurs par état
        Map<EtatBac, Long> parEtat = tousBacs.stream()
                .collect(Collectors.groupingBy(Bac::getEtat, Collectors.counting()));

        // Machines
        List<Machine> machines = machineRepository.findAll();
        long machinesDisponibles = machines.stream().filter(m -> m.getEtat() == Machine.EtatMachine.DISPONIBLE).count();
        long machinesEnCours = machines.stream().filter(m -> m.getEtat() == Machine.EtatMachine.EN_COURS).count();
        long machinesEnMaintenance = machines.stream().filter(m -> m.getEtat() == Machine.EtatMachine.EN_MAINTENANCE).count();

        // Réclamations
      //  long reclamationsOuvertes = reclamationRepository.findByStatut(Reclamation.StatutReclamation.OUVERTE).size();
        long reclamationsUrgentes = reclamationRepository.findAll().stream()
                .filter(r -> r.getType() == TypeReclamation.DECHIRE
                       )
                .count();

        // Activité agents aujourd'hui
        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();
        List<HistoriqueBac> operationsAujourdhui = historiqueBacRepository.findAll().stream()
                .filter(h -> h.getDateOperation().isAfter(debutJournee))
                .collect(Collectors.toList());

        Map<Long, List<HistoriqueBac>> parAgent = operationsAujourdhui.stream()
                .collect(Collectors.groupingBy(h -> h.getAgent().getId()));

        List<DashboardDTO.ActiviteAgentDTO> activiteAgents = parAgent.entrySet().stream()
                .map(e -> {
                    Agent agent = e.getValue().get(0).getAgent();
                    HistoriqueBac derniere = e.getValue().stream()
                            .max(Comparator.comparing(HistoriqueBac::getDateOperation)).orElse(null);
                    return DashboardDTO.ActiviteAgentDTO.builder()
                            .matricule(agent.getMatricule())
                            .nomComplet(agent.getPrenom() + " " + agent.getNom())
                            .nombreOperations(e.getValue().size())
                            .derniereOperation(derniere != null ? derniere.getTypeOperation().name() : null)
                            .build();
                })
                .sorted(Comparator.comparingLong(DashboardDTO.ActiviteAgentDTO::getNombreOperations).reversed())
                .collect(Collectors.toList());

        // Répartition catégories de linge
        Map<String, Long> repartitionCategories = lingeRepository.findAll().stream()
                .collect(Collectors.groupingBy(l -> l.getCategorie().name(), Collectors.counting()));

        return DashboardDTO.builder()
                .bacsEnAttenteCollecte(parEtat.getOrDefault(EtatBac.EN_ATTENTE_COLLECTE, 0L))
                .bacsCollectes(parEtat.getOrDefault(EtatBac.COLLECTE, 0L))
                .bacsEnLavage(parEtat.getOrDefault(EtatBac.EN_LAVAGE, 0L))
                .bacsEnSechage(parEtat.getOrDefault(EtatBac.EN_SECHAGE, 0L))
                .bacsEnPliage(parEtat.getOrDefault(EtatBac.EN_PLIAGE, 0L))
                .bacsEnCollage(parEtat.getOrDefault(EtatBac.EN_COLLAGE, 0L))
                .bacsEnStockage(parEtat.getOrDefault(EtatBac.EN_STOCKAGE, 0L))
                .bacsLivres(parEtat.getOrDefault(EtatBac.LIVRE, 0L))
                .bacsTotaux(tousBacs.size())
                .machinesDisponibles(machinesDisponibles)
                .machinesEnCours(machinesEnCours)
                .machinesEnMaintenance(machinesEnMaintenance)
              //  .reclamationsOuvertes(reclamationsOuvertes)
                .reclamationsUrgentes(reclamationsUrgentes)
                .activiteAgents(activiteAgents)
                .repartitionCategories(repartitionCategories)
                .build();
    }
}
