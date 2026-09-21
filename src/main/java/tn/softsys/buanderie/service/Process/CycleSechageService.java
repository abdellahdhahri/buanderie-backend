package tn.softsys.buanderie.service.Process;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerPersonnelRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;
import tn.softsys.buanderie.dto.Lot.LotScanResponse;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.Reporting.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
public class CycleSechageService {

    private final CycleSechageRepository     cycleRepo;
    private final CycleSechageItemRepository itemRepo;
    private final LotRepository              lotRepo;
    private final MachineRepository          machineRepo;
    private final ProgrammeSechageRepository programmeRepo;
    private final AgentRepository            agentRepository;
    private final LingeRepository            lingeRepository;
    private final HistoriqueLingeRepository  historiqueLingeRepository;
    private final NotificationService        notificationService;
    private final SqlServerPersonnelRepository sqlPersonnelRepo;


    public LotScanResponse scanLot(String codeLot) {

        Lot lot = lotRepo.findByCodeLot(codeLot)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", "code", codeLot));

        ProgrammeSechage programme = suggestProgramme(lot.getCategorie());

        return LotScanResponse.builder()
                .codeLot(lot.getCodeLot())
                .categorie(lot.getCategorie())
                .programmeRecommande(programme.getNom())
                .temperature(programme.getTemperatureCelsius())
                .description(programme.getDescription() != null
                        ? programme.getDescription()
                        + " | Humidité cible : " + programme.getNiveauHumidite() + "%"
                        : "Humidité cible : " + programme.getNiveauHumidite() + "%")
                .duree(programme.getDureeMinutes())
                .build();
    }


    @Transactional
    public CycleSechage startCycle(String codeMachine,
                                    String codeLot,
                                    String matriculeAgent,
                                    Double poidsCharge) {


        Personnel agent = sqlPersonnelRepo.findByCodeBarre(matriculeAgent);

        if (agent == null) {
            throw new ResourceNotFoundException(
                    "Agent", "codeBarre", matriculeAgent);
        }

        Machine machine = machineRepo.findByCode(codeMachine);
        if (machine == null)
            throw new ResourceNotFoundException("Machine", "code", codeMachine);

        //  Vérifier que la machine est un séchoir
        if (machine.getType() != TypeMachine.SECHEUR
                && machine.getType() != TypeMachine.LAVEUR_SECHEUR)
            throw new BusinessException(
                    "La machine [" + codeMachine + "] n'est pas un séchoir. "
                    + "Type actuel : " + machine.getType());

        if (machine.getEtatImmobilisation() != EtatImmobilisation.DISPONIBLE)
            throw new BusinessException(
                    "Machine indisponible — état : " + machine.getEtatImmobilisation());

        Lot lot = lotRepo.findByCodeLot(codeLot)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", "code", codeLot));

        // Vérifier que les linges sont LAVÉS (prêts pour séchage)
        List<Linge> linges = lingeRepository.findByLot_Id(lot.getId());
        if (linges.isEmpty())
            throw new BusinessException("Lot vide : " + codeLot);

        boolean tousLaves = linges.stream()
                .allMatch(l -> l.getEtat() == EtatLinge.LAVE
                            || l.getEtat() == EtatLinge.SECHE); // SECHE si re-séchage
        if (!tousLaves)
            throw new BusinessException(
                    "Certains linges ne sont pas encore lavés. "
                    + "Le séchage nécessite l'état LAVE.");

        // Vérifier capacité
        if (poidsCharge > machine.getCapaciteKg())
            throw new BusinessException(
                    "Capacité machine insuffisante : "
                    + machine.getCapaciteKg() + " kg max, "
                    + poidsCharge + " kg demandés.");

        ProgrammeSechage programme = suggestProgramme(lot.getCategorie());

        // Créer le cycle
        CycleSechage cycle = cycleRepo.save(
                CycleSechage.builder()
                        .machine(machine)
                        .programmeSechage(programme)
                        .personnelId(agent.getId())
                        .poidsCharge(poidsCharge)
                        .dateDebut(LocalDateTime.now())
                        .statut(StatutCycle.EN_COURS)
                        .build()
        );

        for (Linge linge : linges) {
            EtatLinge ancien = linge.getEtat();
            linge.setEtat(EtatLinge.EN_SECHAGE);

            historiqueLingeRepository.save(HistoriqueLinge.builder()
                    .linge(linge)
                    .etatAvant(ancien)
                    .etatApres(EtatLinge.EN_SECHAGE)
                    .personnelId(agent.getId())
                    .typeOperation(TypeOperation.DEMARRAGE_SECHAGE)
                    .dateOperation(LocalDateTime.now())
                    .observations("Passage en séchage — cycle " + cycle.getNumeroCycle()
                            + " — programme " + programme.getNom())
                    .build());
        }
        lingeRepository.saveAll(linges);

        // Ajouter le lot au cycle
        addLotToCycleInternal(cycle, lot, poidsCharge);

        // Marquer machine EN_COURS
        machine.setEtatImmobilisation(EtatImmobilisation.EN_COURS);
        machineRepo.save(machine);

        lot.setPoidsActuel(0.0);
        lotRepo.save(lot);

        return cycle;
    }

    //  AJOUTER UN LOT À UN CYCLE EN COURS
    //    (plusieurs lots dans la même sécheuse)
    @Transactional
    public CycleSechage addLotToCycle(Long cycleId,
                                       String codeLot,
                                       Double poidsCharge) {

        CycleSechage cycle = cycleRepo.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CycleSechage", "id", cycleId));

        if (cycle.getStatut() != StatutCycle.EN_COURS)
            throw new BusinessException(
                    "Cycle non actif — statut : " + cycle.getStatut());

        Lot lot = lotRepo.findByCodeLot(codeLot)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", "code", codeLot));

        double nouveauPoids = cycle.getPoidsCharge() + poidsCharge;
        if (nouveauPoids > cycle.getMachine().getCapaciteKg())
            throw new BusinessException("Capacité dépassée : "
                    + nouveauPoids + " kg > "
                    + cycle.getMachine().getCapaciteKg() + " kg");

        // Linges → EN_SECHAGE
        List<Linge> linges = lingeRepository.findByLot_CodeLot(codeLot);
        for (Linge linge : linges) {
            EtatLinge ancien = linge.getEtat();
            linge.setEtat(EtatLinge.EN_SECHAGE);
            historiqueLingeRepository.save(HistoriqueLinge.builder()
                    .linge(linge).etatAvant(ancien).etatApres(EtatLinge.EN_SECHAGE)
                    //.agent(cycle.getAgent())
                    .typeOperation(TypeOperation.DEMARRAGE_SECHAGE)
                    .dateOperation(LocalDateTime.now())
                    .observations("Ajout lot au cycle " + cycle.getNumeroCycle())
                    .build());
        }
        lingeRepository.saveAll(linges);

        cycle.setPoidsCharge(nouveauPoids);
        addLotToCycleInternal(cycle, lot, poidsCharge);

        return cycleRepo.save(cycle);
    }

    // TERMINER LE CYCLE
    @Transactional
    public CycleSechage finishCycle(Long cycleId) {

        CycleSechage cycle = cycleRepo.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CycleSechage", "id", cycleId));

        if (cycle.getStatut() != StatutCycle.EN_COURS)
            throw new BusinessException(
                    "Cycle déjà terminé — statut : " + cycle.getStatut());

        cycle.setStatut(StatutCycle.TERMINE);
        cycle.setDateFin(LocalDateTime.now());

        // Libérer la machine
        Machine machine = cycle.getMachine();
        machine.setEtatImmobilisation(EtatImmobilisation.DISPONIBLE.DISPONIBLE);
        machineRepo.save(machine);

        List<CycleSechageItem> items = itemRepo.findByCycleSechage(cycle);

        for (CycleSechageItem item : items) {
            Lot lot = lotRepo.findById(item.getSourceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lot", "id", item.getSourceId()));
            lot.setEtat(EtatLot.EN_TRI);
            List<Linge> linges = lingeRepository.findByLot_CodeLot(lot.getCodeLot());

            for (Linge linge : linges) {
                EtatLinge ancien = linge.getEtat();
                linge.setEtat(EtatLinge.SECHE);
                linge.setLot(null);

                // Notification fin de vie si besoin
                if (linge.getDureVie() != null
                        && linge.getNbLavage() >= linge.getDureVie()) {
                    notificationService.notifierFinDeVie(linge);
                }

                historiqueLingeRepository.save(HistoriqueLinge.builder()
                        .linge(linge)
                        .etatAvant(ancien)
                        .etatApres(EtatLinge.SECHE)
                        //.agent(cycle.getAgent())
                        .typeOperation(TypeOperation.FIN_SECHAGE)
                        .dateOperation(LocalDateTime.now())
                        .observations("Fin séchage — cycle " + cycle.getNumeroCycle()
                                + " — programme " + cycle.getProgrammeSechage().getNom())
                        .build());
            }
            lingeRepository.saveAll(linges);

            // Vider le lot
            lot.setPoidsActuel(0.0);
            lot.setEtat(EtatLot.EN_TRI);
            lotRepo.save(lot);
        }

        return cycleRepo.save(cycle);
    }

    // UTILITAIRES PRIVÉS

    private ProgrammeSechage suggestProgramme(CategLinge categorie) {
        return programmeRepo.findByCategoriesContaining(categorie)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProgrammeSechage", "categorie", categorie.name()));
    }

    private void addLotToCycleInternal(CycleSechage cycle, Lot lot, Double poids) {
        itemRepo.save(CycleSechageItem.builder()
                .cycleSechage(cycle)
                .typeSource(TypeSource.LOT_TRI)
                .sourceId(lot.getId())
                .poidsCharge(poids)
                .build());
        lot.setPoidsActuel(0.0);
        lotRepo.save(lot);
    }
}
