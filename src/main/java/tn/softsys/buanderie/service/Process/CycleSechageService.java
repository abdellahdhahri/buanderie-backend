package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.service.referentiel.CubixMachineEtatService;

import tn.softsys.buanderie.dto.BacScanResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.service.management.ParcoursService;
import java.util.LinkedHashSet;
import java.util.Set;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.Reporting.NotificationService;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class CycleSechageService {

    private final CycleSechageRepository     cycleRepo;
    private final CycleSechageItemRepository itemRepo;
    private final MachineRepository          machineRepo;
    private final CubixMachineEtatService    cubixMachineEtat;
    private final ProgrammeSechageRepository programmeRepo;
    private final AgentRepository            agentRepository;
    private final LingeRepository            lingeRepository;
    private final HistoriqueLingeRepository  historiqueLingeRepository;
    private final NotificationService        notificationService;
    private final PersonnelRepository personnelRepository;
    private final ParcoursService parcoursService;
    private final BacRepository bacRepository;






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
            terminerBac(item, cycle);
        }

        cubixMachineEtat.arreter(machine);

        return cycleRepo.save(cycle);
    }

    // ------------------------------------------------------------------
    //  SÉCHAGE D'UN BAC DE TRI (circuit blanc)
    // ------------------------------------------------------------------

    public BacScanResponse scanBac(String codeBac) {
        parcoursService.exigerActive(CodeEtape.SECHAGE);

        Bac bac = resoudreBacDeTri(codeBac);
        ProgrammeSechage programme = programmePourBac(bac);

        return BacScanResponse.builder()
                .codeBac(bac.getCode())
                .categorie(bac.getCategorie())
                .poids(bac.getPoids())
                .poidsBalance(bac.getPoidsBalance())
                .nbLinges(lingeRepository.findByBacId(bac.getId()).size())
                .programmeId(programme.getId())
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
    public CycleSechage startCycleBac(String codeMachine,
                                      String codeBac,
                                      String matriculeAgent,
                                      Double poidsCharge) {
        parcoursService.exigerActive(CodeEtape.SECHAGE);

        Personnel agent = matriculeAgent == null ? null : personnelRepository.findByCodeBarre(matriculeAgent);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "codeBarre", matriculeAgent);
        }

        Machine machine = machineRepo.findByCode(codeMachine);
        if (machine == null) {
            throw new ResourceNotFoundException("Machine", "code", codeMachine);
        }
        if (machine.getEtatTechnique() == tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.EN_PANNE
                || machine.getEtatTechnique() == tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.EN_COURS_DE_REPARATION) {
            throw new BusinessException("La machine " + codeMachine + " est en panne : démarrage impossible.");
        }
        if (machine.getType() != TypeMachine.SECHEUR
                && machine.getType() != TypeMachine.LAVEUR_SECHEUR) {
            throw new BusinessException(
                    "La machine [" + codeMachine + "] n'est pas un séchoir. Type actuel : " + machine.getType());
        }
        if (machine.getEtatImmobilisation() != EtatImmobilisation.DISPONIBLE) {
            throw new BusinessException("Machine indisponible — état : " + machine.getEtatImmobilisation());
        }

        Bac bac = resoudreBacDeTri(codeBac);
        exigerBacPretPourSechage(bac);

        List<Linge> linges = lingeRepository.findByBacId(bac.getId());
        if (linges.isEmpty() && !porteDesQuantites(bac)) {
            throw new BusinessException("Bac vide : " + bac.getCode());
        }
        verifierEtatsLinges(linges);

        ProgrammeSechage programme = programmePourBac(bac);
        double poids = poidsDuBac(bac, poidsCharge);

        CycleSechage cycle = cycleRepo.save(
                CycleSechage.builder()
                        .machine(machine)
                        .programmeSechage(programme)
                        .personnelId(agent.getId())
                        .poidsCharge(poids)
                        .dateDebut(LocalDateTime.now())
                        .statut(StatutCycle.EN_COURS)
                        .build());

        mettreBacEnSechage(bac, linges, cycle, agent.getId(),
                "Passage en séchage — cycle " + cycle.getNumeroCycle() + " — programme " + programme.getNom());
        ajouterBacAuCycleInterne(cycle, bac, poids);

        machine.setEtatImmobilisation(EtatImmobilisation.EN_COURS);
        machineRepo.save(machine);

        cubixMachineEtat.demarrerSechage(machine, linges, programme.getDureeMinutes(), agent);

        return cycle;
    }

    @Transactional
    public CycleSechage addBacToCycle(Long cycleId, String codeBac, Double poidsCharge) {
        parcoursService.exigerActive(CodeEtape.SECHAGE);

        CycleSechage cycle = cycleRepo.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("CycleSechage", "id", cycleId));
        if (cycle.getStatut() != StatutCycle.EN_COURS) {
            throw new BusinessException("Cycle non actif — statut : " + cycle.getStatut());
        }

        Bac bac = resoudreBacDeTri(codeBac);
        exigerBacPretPourSechage(bac);

        List<Linge> linges = lingeRepository.findByBacId(bac.getId());
        if (linges.isEmpty() && !porteDesQuantites(bac)) {
            throw new BusinessException("Bac vide : " + bac.getCode());
        }
        verifierEtatsLinges(linges);

        double poids = poidsDuBac(bac, poidsCharge);
        double nouveauPoids = cycle.getPoidsCharge() + poids;
        Double capacite = cycle.getMachine().getCapaciteKg();
        if (capacite != null && nouveauPoids > capacite) {
            throw new BusinessException("Capacité dépassée : " + nouveauPoids + " kg > " + capacite + " kg");
        }

        mettreBacEnSechage(bac, linges, cycle, cycle.getPersonnelId(),
                "Ajout du bac " + bac.getCode() + " au cycle " + cycle.getNumeroCycle());

        cycle.setPoidsCharge(nouveauPoids);
        ajouterBacAuCycleInterne(cycle, bac, poids);

        return cycleRepo.save(cycle);
    }

    private Bac resoudreBacDeTri(String codeBac) {
        if (codeBac == null || codeBac.isBlank()) {
            throw new BusinessException("Le code du bac est obligatoire.");
        }
        String code = codeBac.trim();
        Bac bac = bacRepository.findByCode(code)
                .or(() -> bacRepository.findByRfidTag(code))
                .orElseThrow(() -> new ResourceNotFoundException("Bac", "code", code));

        if (bac.getUsage() != UsageBac.TRI) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » n'est pas un bac de tri (usage : " + bac.getUsage() + ").");
        }
        return bac;
    }

    private boolean porteDesQuantites(Bac bac) {
        return bac.getQuantiteDeclaree() != null && bac.getQuantiteDeclaree() > 0;
    }

    private void exigerBacPretPourSechage(Bac bac) {
        if (bac.getEtat() != EtatBac.EN_ATTENTE_SECHAGE) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » n'est pas prêt pour le séchage (état : " + bac.getEtat() + ").");
        }
    }

    private ProgrammeSechage programmePourBac(Bac bac) {
        if (bac.getCategorie() == null) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » n'a pas de catégorie : impossible de choisir le programme de séchage.");
        }
        return suggestProgramme(bac.getCategorie());
    }

    private double poidsDuBac(Bac bac, Double poidsCharge) {
        if (poidsCharge != null) {
            return poidsCharge;
        }
        if (bac.getPoidsBalance() != null) {
            return bac.getPoidsBalance();
        }
        return bac.getPoids() != null ? bac.getPoids() : 0.0;
    }

    private void verifierEtatsLinges(List<Linge> linges) {
        Set<EtatLinge> etatsAcceptes = new LinkedHashSet<>(parcoursService.etatsAcceptes(CodeEtape.SECHAGE));
        if (etatsAcceptes.isEmpty()) {
            etatsAcceptes.add(EtatLinge.LAVE);
        }
        etatsAcceptes.add(EtatLinge.SECHE);
        boolean tousPrets = linges.stream().allMatch(l -> etatsAcceptes.contains(l.getEtat()));
        if (!tousPrets) {
            throw new BusinessException(
                    "Certains linges ne sont pas prêts pour le séchage. États acceptés : "
                            + String.join(", ", etatsAcceptes.stream().map(Enum::name).toList()) + ".");
        }
    }

    private void mettreBacEnSechage(Bac bac, List<Linge> linges, CycleSechage cycle,
                                    Long personnelId, String observation) {
        for (Linge linge : linges) {
            EtatLinge ancien = linge.getEtat();
            linge.setEtat(EtatLinge.EN_SECHAGE);
            historiqueLingeRepository.save(HistoriqueLinge.builder()
                    .linge(linge)
                    .etatAvant(ancien)
                    .etatApres(EtatLinge.EN_SECHAGE)
                    .personnelId(personnelId)
                    .typeOperation(TypeOperation.DEMARRAGE_SECHAGE)
                    .dateOperation(LocalDateTime.now())
                    .observations(observation)
                    .build());
        }
        lingeRepository.saveAll(linges);

        bac.setEtat(EtatBac.EN_SECHAGE);
        bacRepository.save(bac);
    }

    private void ajouterBacAuCycleInterne(CycleSechage cycle, Bac bac, double poids) {
        itemRepo.save(CycleSechageItem.builder()
                .cycleSechage(cycle)
                .typeSource(TypeSource.BAC)
                .sourceId(bac.getId())
                .poidsCharge(poids)
                .build());
    }

    private void terminerBac(CycleSechageItem item, CycleSechage cycle) {
        Bac bac = bacRepository.findById(item.getSourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Bac", "id", item.getSourceId()));

        List<Linge> linges = lingeRepository.findByBacId(bac.getId());

        for (Linge linge : linges) {
            EtatLinge ancien = linge.getEtat();
            linge.setEtat(EtatLinge.SECHE);
            linge.setBac(null);

            if (linge.getDureVie() != null && linge.getNbLavage() >= linge.getDureVie()) {
                notificationService.notifierFinDeVie(linge);
            }

            historiqueLingeRepository.save(HistoriqueLinge.builder()
                    .linge(linge)
                    .etatAvant(ancien)
                    .etatApres(EtatLinge.SECHE)
                    .personnelId(cycle.getPersonnelId())
                    .typeOperation(TypeOperation.FIN_SECHAGE)
                    .dateOperation(LocalDateTime.now())
                    .observations("Fin séchage — cycle " + cycle.getNumeroCycle()
                            + " — programme " + cycle.getProgrammeSechage().getNom())
                    .build());
        }
        lingeRepository.saveAll(linges);

        bac.setPoids(0.0);
        bac.setPoidsBalance(null);
        bac.setSource(null);
        bac.setQuantiteDeclaree(null);
        bac.setDetailQuantites(null);
        bac.setEtat(EtatBac.VIDE);
        bacRepository.save(bac);
    }

    // UTILITAIRES PRIVÉS

    private ProgrammeSechage suggestProgramme(CategLinge categorie) {
        return programmeRepo.findByCategoriesContaining(categorie)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProgrammeSechage", "categorie", categorie.name()));
    }

}