package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.entity.enums.GroupeLavage;

import tn.softsys.buanderie.service.referentiel.CubixMachineEtatService;

import tn.softsys.buanderie.dto.BacScanResponse;

import tn.softsys.buanderie.service.management.ProfilExploitationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.dto.PersonnelDto;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.service.referentiel.PersonnelReferentielService;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.Reporting.NotificationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import tn.softsys.buanderie.service.management.ParcoursService;

@Service
@RequiredArgsConstructor
public class CycleLavageService {

    private final CycleLavageRepository cycleRepo;
    private final MachineRepository machineRepo;
    private final CubixMachineEtatService cubixMachineEtat;
    private final ProgrammeLavageRepository programmeRepo;
    private final CycleLavageItemRepository itemRepo;
    private final AgentRepository agentRepository;
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final NotificationService notificationService;
    private final ProfilExploitationService profilExploitationService;
    private final PersonnelReferentielService personnelReferentielService;
    private final PersonnelRepository personnelRepository;
    private final ParcoursService parcoursService;
    private final BacRepository bacRepository;



    private record SuggestionProgramme(ProgrammeLavage programme, boolean parDefaut) {}

    /**
     * Programme de la catégorie dominante du lot ; à défaut, programme par défaut.
     */
    private GroupeLavage groupeDuBac(Bac bac) {
        if (bac.getGroupeLavage() != null) return bac.getGroupeLavage();
        long couleur = lingeRepository.findByBacId(bac.getId()).stream()
                .filter(l -> (l.getGroupeLavage() != null ? l.getGroupeLavage()
                        : GroupeLavage.depuisDesignation(l.getDesignation())) == GroupeLavage.COULEUR)
                .count();
        long total = lingeRepository.findByBacId(bac.getId()).size();
        return total > 0 && couleur * 2 > total ? GroupeLavage.COULEUR : GroupeLavage.BLANC;
    }

    private SuggestionProgramme resoudreProgramme(Bac bac) {
        GroupeLavage groupe = groupeDuBac(bac);
        Optional<ProgrammeLavage> deGroupe =
                programmeRepo.findFirstByGroupeLavageAndTypeOrderByIdAsc(groupe, TypeProgramme.LAVAGE);
        if (deGroupe.isPresent()) {
            return new SuggestionProgramme(deGroupe.get(), false);
        }
        return resoudreProgramme(bac.getCategorie());
    }

    private SuggestionProgramme resoudreProgramme(CategLinge categorie) {
        if (categorie != null) {
            Optional<ProgrammeLavage> deCategorie =
                    programmeRepo.findFirstByCategoriesContainingAndTypeOrderByIdAsc(
                            categorie, TypeProgramme.LAVAGE);
            if (deCategorie.isPresent()) {
                return new SuggestionProgramme(deCategorie.get(), false);
            }
        }

        ProgrammeLavage parDefaut = programmeRepo
                .findFirstByParDefautTrueAndTypeOrderByIdAsc(TypeProgramme.LAVAGE)
                .orElseThrow(() -> new BusinessException(
                        "Aucun programme de lavage pour la catégorie "
                                + (categorie != null ? categorie : "(non définie)")
                                + " et aucun programme par défaut n'est défini. "
                                + "Définissez un programme de lavage par défaut."));

        return new SuggestionProgramme(parDefaut, true);
    }

    private ProgrammeLavage suggestProgramme(CategLinge categorie) {
        return resoudreProgramme(categorie).programme();
    }




    public CycleLavage finishCycle(Long cycleId) {
        return finishCycle(cycleId, null);
    }

    /**
     * Fin de cycle. Pour un cycle de désinfection (bac ou sac infecté) et si le séchage
     * est actif, l'agent scanne le bac de déchargement : le bac devient le contenant
     * des linges désinfectés (ou de leurs quantités) pour le séchage.
     */
    @Transactional
    public CycleLavage finishCycle(Long cycleId, String codeBac) {

        CycleLavage cycle = cycleRepo.findById(cycleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cycle non trouvé"));

        if (cycle.getStatut() != StatutCycle.EN_COURS) {
            throw new BusinessException(
                    "Le cycle n'est pas en cours. État actuel : "
                            + cycle.getStatut()
            );
        }

        cycle.setStatut(StatutCycle.TERMINE);
        cycle.setDateFin(LocalDateTime.now());

        Machine machine = cycle.getMachine();

        List<CycleLavageItem> items =
                itemRepo.findByCycleLavage(cycle);

        List<Bac> bacsDechargement = new ArrayList<>();

        for (CycleLavageItem item : items) {


            if (item.getTypeSource() == TypeSource.BAC) {
                continue;
            }

            if (item.getTypeSource() == TypeSource.BAC_INFECTE) {

                Bac bac = bacRepository.findById(item.getSourceId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Bac infecté introuvable"
                                ));

                if (bac.getEtat() != EtatBac.EN_LAVAGE) {
                    throw new BusinessException(
                            "Le bac " + bac.getCode() + " n'est pas EN_LAVAGE."
                    );
                }

                List<Linge> lingesBac = lingeRepository.findByBacId(bac.getId());
                terminerLingesInfectes(lingesBac, cycle, "bac " + bac.getCode(), cycle.getPersonnelId());

                Bac bacSechage = null;
                if (parcoursService.estActive(CodeEtape.SECHAGE)) {
                    bacSechage = resoudreBacDechargement(codeBac, bacsDechargement);
                    remplirBacDechargement(bacSechage, lingesBac, null,
                            item.getPoidsCharge(), "BAC:" + bac.getCode());
                }

                // Déchargement : les linges quittent le bac (et rejoignent le lot scanné)
                for (Linge linge : lingesBac) {
                    linge.setBac(null);
                    if (bacSechage != null) {
                        linge.setBac(bacSechage);
                    }
                }
                lingeRepository.saveAll(lingesBac);

                bac.setEtat(EtatBac.VIDE);
                bac.setPoids(null);
                bacRepository.save(bac);
            }
        }

        if (items.stream().anyMatch(i -> i.getTypeSource() == TypeSource.BAC)) {
            terminerLingesBlanches(cycle, items, codeBac);
        }

        machine.setEtatImmobilisation(EtatImmobilisation.DISPONIBLE);
        machineRepo.save(machine);

        cubixMachineEtat.arreter(machine);

        return cycleRepo.save(cycle);
    }



    // ------------------------------------------------------------------
    //  LAVAGE D'UN BAC DE TRI (circuit blanc)
    // ------------------------------------------------------------------

    public BacScanResponse scanBac(String codeBac) {
        Bac bac = resoudreBacDeTri(codeBac);
        SuggestionProgramme suggestion = resoudreProgramme(bac);
        ProgrammeLavage programme = suggestion.programme();

        return BacScanResponse.builder()
                .codeBac(bac.getCode())
                .categorie(bac.getCategorie())
                .groupeLavage(groupeDuBac(bac).name())
                .poids(bac.getPoids())
                .poidsBalance(bac.getPoidsBalance())
                .nbLinges(lingeRepository.findByBacId(bac.getId()).size())
                .programmeId(programme.getId())
                .programmeRecommande(programme.getNom())
                .temperature(programme.getTemperatureCelsius())
                .description(programme.getDescription())
                .vitesse(programme.getVitesseEssorageRpm())
                .duree(programme.getDureeMinutes())
                .origineProgramme(suggestion.parDefaut() ? "DEFAUT" : "GROUPE")
                .programmeParDefaut(suggestion.parDefaut())
                .build();
    }

    @Transactional
    public CycleLavage startCycleBac(String codeMachine,
                                     String codeBac,
                                     String matriculeAgent,
                                     Double poidsCharge) {
        Personnel agent = matriculeAgent == null ? null : personnelRepository.findByCodeBarre(matriculeAgent);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", matriculeAgent);
        }

        Machine machine = machineRepo.findByCode(codeMachine);
        if (machine == null) {
            throw new ResourceNotFoundException("Machine", "code", codeMachine);
        }
        if (machine.getEtatTechnique() == tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.EN_PANNE
                || machine.getEtatTechnique() == tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.EN_COURS_DE_REPARATION) {
            throw new BusinessException("La machine " + codeMachine + " est en panne : démarrage impossible.");
        }
        if (machine.getEtatImmobilisation() != EtatImmobilisation.DISPONIBLE) {
            throw new BusinessException("La machine " + codeMachine + " n'est pas disponible.");
        }

        Bac bac = resoudreBacDeTri(codeBac);
        exigerBacPretPourLavage(bac);

        List<Linge> linges = lingeRepository.findByBacId(bac.getId());
        if (linges.isEmpty()) {
            throw new BusinessException("Le bac « " + bac.getCode() + " » ne contient aucun linge.");
        }

        double poids = poidsDuBac(bac, poidsCharge);
        ProgrammeLavage programme = resoudreProgramme(bac).programme();

        CycleLavage cycle = cycleRepo.save(
                CycleLavage.builder()
                        .machine(machine)
                        .programmeLavage(programme)
                        .personnelId(agent.getId())
                        .poidsCharge(poids)
                        .dateDebut(LocalDateTime.now())
                        .statut(StatutCycle.EN_COURS)
                        .build());

        mettreBacEnLavage(bac, linges, agent.getId(), cycle.getId());
        ajouterBacAuCycle(cycle, bac, poids, linges.size());

        machine.setEtatImmobilisation(EtatImmobilisation.EN_COURS);
        machineRepo.save(machine);

        cubixMachineEtat.demarrerLavage(machine, linges, programme.getDureeMinutes(), agent);

        return cycle;
    }

    @Transactional
    public CycleLavage addBacToCycle(Long cycleId, String codeBac, Double poidsCharge) {
        CycleLavage cycle = cycleRepo.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle non trouvé"));
        if (cycle.getStatut() != StatutCycle.EN_COURS) {
            throw new BusinessException("Le cycle n'est pas en cours. État actuel : " + cycle.getStatut());
        }

        Bac bac = resoudreBacDeTri(codeBac);
        exigerBacPretPourLavage(bac);

        List<Linge> linges = lingeRepository.findByBacId(bac.getId());
        if (linges.isEmpty()) {
            throw new BusinessException("Le bac « " + bac.getCode() + " » ne contient aucun linge.");
        }

        double poids = poidsDuBac(bac, poidsCharge);

        mettreBacEnLavage(bac, linges, cycle.getPersonnelId(), cycle.getId());

        double nouveauPoids = cycle.getPoidsCharge() + poids;
        cycle.setPoidsCharge(nouveauPoids);
        ajouterBacAuCycle(cycle, bac, poids, linges.size());

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
        if (bac.getTypebac() == TypeBac.INFECTE) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » est infecté : il se lave par le lavage de désinfection.");
        }
        return bac;
    }

    private void exigerBacPretPourLavage(Bac bac) {
        if (bac.getEtat() != EtatBac.EN_TRI && bac.getEtat() != EtatBac.PRET_LAVAGE) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » n'est pas prêt pour le lavage (état : " + bac.getEtat() + ").");
        }
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

    private void mettreBacEnLavage(Bac bac, List<Linge> linges, Long personnelId, Long cycleId) {
        for (Linge linge : linges) {
            EtatLinge ancienEtat = linge.getEtat();
            linge.setEtat(EtatLinge.EN_LAVAGE);
            linge.setBac(null);
            linge.setCycleLavageId(cycleId);

            if (linge.getDureVie() != null && linge.getNbLavage() + 1 >= linge.getDureVie()) {
                notificationService.notifierFinDeVie(linge);
            }

            historiqueLingeRepository.save(
                    HistoriqueLinge.builder()
                            .linge(linge)
                            .etatAvant(ancienEtat)
                            .etatApres(EtatLinge.EN_LAVAGE)
                            .personnelId(personnelId)
                            .typeOperation(TypeOperation.DEMARRAGE_LAVAGE)
                            .dateOperation(LocalDateTime.now())
                            .observations("Passage en lavage — bac " + bac.getCode())
                            .build());
        }
        lingeRepository.saveAll(linges);

        // Le linge est dans la machine : le bac de tri est libéré immédiatement.
        bac.setEtat(EtatBac.VIDE);
        bac.setPoids(0.0);
        bac.setPoidsBalance(null);
        bac.setGroupeLavage(null);
        bacRepository.save(bac);
    }

    /**
     * Fin de lavage (circuit blanc) : les linges du cycle sont déposés dans le bac scanné
     * (n'importe quel bac, sans contrôle) qui devient prêt pour le séchage.
     */
    private void terminerLingesBlanches(CycleLavage cycle, List<CycleLavageItem> items, String codeBac) {
        List<Linge> linges = new ArrayList<>(lingeRepository.findByCycleLavageId(cycle.getId()));

        // Compatibilité : cycles démarrés avant ce changement (linges encore dans leur bac)
        for (CycleLavageItem item : items) {
            if (item.getTypeSource() != TypeSource.BAC || item.getSourceId() == null) continue;
            Bac bacSource = bacRepository.findById(item.getSourceId()).orElse(null);
            if (bacSource != null && bacSource.getEtat() == EtatBac.EN_LAVAGE) {
                for (Linge l : lingeRepository.findByBacId(bacSource.getId())) {
                    if (linges.stream().noneMatch(x -> x.getId().equals(l.getId()))) {
                        linges.add(l);
                    }
                }
                bacSource.setEtat(EtatBac.VIDE);
                bacSource.setPoids(0.0);
                bacSource.setPoidsBalance(null);
                bacSource.setGroupeLavage(null);
                bacRepository.save(bacSource);
            }
        }

        if (linges.isEmpty()) return;

        boolean sechageActif = parcoursService.estActive(CodeEtape.SECHAGE);
        Bac dechargement = null;
        if (sechageActif) {
            if (codeBac == null || codeBac.isBlank()) {
                throw new BusinessException(
                        "Scannez le bac de déchargement pour terminer ce cycle de lavage.");
            }
            String code = codeBac.trim();
            dechargement = bacRepository.findByCode(code)
                    .or(() -> bacRepository.findByRfidTag(code))
                    .orElseThrow(() -> new ResourceNotFoundException("Bac", "code", code));
        }

        double poidsTotal = 0;
        for (Linge linge : linges) {
            EtatLinge ancienEtat = linge.getEtat();
            linge.setEtat(EtatLinge.LAVE);
            linge.setNbLavage(linge.getNbLavage() + 1);
            linge.setBac(dechargement);
            poidsTotal += linge.getPoids() == null ? 0 : linge.getPoids();

            historiqueLingeRepository.save(
                    HistoriqueLinge.builder()
                            .linge(linge)
                            .etatAvant(ancienEtat)
                            .etatApres(EtatLinge.LAVE)
                            .personnelId(cycle.getPersonnelId())
                            .typeOperation(TypeOperation.FIN_LAVAGE)
                            .dateOperation(LocalDateTime.now())
                            .observations("Fin du cycle de lavage"
                                    + (dechargement != null ? " — déchargé dans le bac " + dechargement.getCode() : ""))
                            .build());
        }
        lingeRepository.saveAll(linges);

        if (dechargement != null) {
            double base = dechargement.getEtat() == EtatBac.VIDE || dechargement.getPoids() == null
                    ? 0.0 : dechargement.getPoids();
            long couleur = linges.stream()
                    .filter(l -> (l.getGroupeLavage() != null ? l.getGroupeLavage()
                            : GroupeLavage.depuisDesignation(l.getDesignation())) == GroupeLavage.COULEUR)
                    .count();
            dechargement.setGroupeLavage(couleur * 2 > linges.size() ? GroupeLavage.COULEUR : GroupeLavage.BLANC);
            dechargement.setEtat(EtatBac.EN_ATTENTE_SECHAGE);
            // Les linges n'ont souvent pas de poids unitaire : on reprend le poids chargé du cycle.
            double poidsLave = poidsTotal > 0 ? poidsTotal
                    : (cycle.getPoidsCharge() != null ? cycle.getPoidsCharge() : 0.0);
            dechargement.setPoids(base + poidsLave);
            dechargement.setPoidsBalance(null);
            dechargement.setSource("CYCLE:" + cycle.getId());
            bacRepository.save(dechargement);
        }
    }

    private void ajouterBacAuCycle(CycleLavage cycle, Bac bac, double poids, int quantite) {
        itemRepo.save(
                CycleLavageItem.builder()
                        .cycleLavage(cycle)
                        .typeSource(TypeSource.BAC)
                        .sourceId(bac.getId())
                        .poidsCharge(poids)
                        .quantiteChargee(quantite)
                        .build());
    }


//    private void historiserLavage(Linge linge, Agent agent, EtatLinge nouvelEtat) {
//        HistoriqueLinge historique = HistoriqueLinge.builder()
//                .linge(linge)
//                .etatAvant(linge.getEtat())
//                .etatApres(nouvelEtat)
//                .agent(agent)
//                .typeOperation(TypeOperation.DEMARRAGE_LAVAGE)
//                .dateOperation(LocalDateTime.now())
//                .observations("Passage en lavage")
//                .dateOperation(LocalDateTime.now())
//                //.collecte(linge.getc())
//               // .sacInfecte(linge.getSacInfecte())
//                .build();
//        historiqueLingeRepository.save(historique);
//        linge.setEtat(nouvelEtat);
//        lingeRepository.save(linge);
//    }

    public List<CycleLavage> getAllCycles() {
        return cycleRepo.findAllByOrderByDateDebutDesc();
    }

    public CycleLavage getCycleById(Long id) {
        return cycleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CycleLavage", "id", id));
    }

    public List<Map<String, Object>> getLingeDuCycle(Long cycleId) {
        CycleLavage cycle = getCycleById(cycleId);
        List<CycleLavageItem> items = itemRepo.findByCycleLavage_Id(cycleId);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        boolean blancDejaListe = false;

        for (CycleLavageItem item : items) {
            if (item.getSourceId() == null) continue;

            List<Linge> linges;
            if (item.getTypeSource() == TypeSource.BAC) {
                if (blancDejaListe) {
                    linges = new ArrayList<>();
                } else {
                    blancDejaListe = true;
                    linges = lingeRepository.findByCycleLavageId(cycleId);
                    if (linges.isEmpty()) {
                        linges = lingeRepository.findByBacId(item.getSourceId());
                    }
                }
            } else if (item.getTypeSource() == TypeSource.BAC_INFECTE) {
                linges = lingeRepository.findByBacId(item.getSourceId());
            } else {
                linges = new ArrayList<>();
            }

            for (Linge l : linges) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("lingeId",   l.getId());
                m.put("codeBarre", l.getCodeBarre());
                m.put("rfidTag",   l.getRfidTag());
                m.put("categorie", l.getCategorie() != null ? l.getCategorie().name() : null);
                m.put("etat",      l.getEtat() != null ? l.getEtat().name() : null);
                m.put("poids",     l.getPoids());
                m.put("bacId",     item.getSourceId());
                m.put("itemId",    item.getId());
                result.add(m);
            }
        }
        return result;
    }




    //===========================================================//



    // ------------------------------------------------------------------
    //  LAVAGE DIRECT D'UN BAC INFECTÉ
    // ------------------------------------------------------------------

    @Transactional
    public CycleLavage startCycleBacInfecte(
            String codeMachine,
            String codeBac,
            String matriculeAgent,
            Double poidsCharge) {

        profilExploitationService.exigerCircuitInfecte();

        Personnel agent = personnelRepository.findByCodeBarre(matriculeAgent);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", matriculeAgent);
        }

        Bac bac = bacRepository.findByCode(codeBac)
                .or(() -> bacRepository.findByRfidTag(codeBac))
                .orElseThrow(() -> new ResourceNotFoundException("Bac", "code", codeBac));

        if (bac.getTypebac() != TypeBac.INFECTE) {
            throw new BusinessException("Le bac " + bac.getCode() + " n'est pas un bac infecté.");
        }

        if (bac.getEtat() != EtatBac.EN_RECEPTION) {
            throw new BusinessException(
                    "Le bac doit être EN_RECEPTION (état actuel : " + bac.getEtat()
                            + "). Réceptionnez-le d'abord.");
        }

        List<Linge> linges = lingeRepository.findByBacId(bac.getId());
        if (linges.isEmpty()) {
            throw new BusinessException(
                    "Le bac " + bac.getCode() + " ne contient aucun linge déclaré.");
        }

        Machine machine = machineRepo.findByCode(codeMachine);
        if (machine == null) {
            throw new ResourceNotFoundException("Machine", "code", codeMachine);
        }
        if (machine.getEtatTechnique() == tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.EN_PANNE
                || machine.getEtatTechnique() == tn.softsys.buanderie.entity.enums.EtatTechniqueMachine.EN_COURS_DE_REPARATION) {
            throw new BusinessException("La machine " + codeMachine + " est en panne : démarrage impossible.");
        }
        if (machine.getEtatImmobilisation() != EtatImmobilisation.DISPONIBLE) {
            throw new BusinessException(
                    "La machine " + codeMachine + " n'est pas disponible.");
        }

        ProgrammeLavage programme = programmeDesinfection();

        double poids = poidsCharge != null
                ? poidsCharge
                : (bac.getPoids() != null ? bac.getPoids() : 0.0);

        CycleLavage cycle = cycleRepo.save(
                CycleLavage.builder()
                        .machine(machine)
                        .programmeLavage(programme)
                        .personnelId(agent.getId())
                        .poidsCharge(poids)
                        .dateDebut(LocalDateTime.now())
                        .statut(StatutCycle.EN_COURS)
                        .build());

        itemRepo.save(
                CycleLavageItem.builder()
                        .cycleLavage(cycle)
                        .typeSource(TypeSource.BAC_INFECTE)
                        .sourceId(bac.getId())
                        .poidsCharge(poids)
                        .quantiteChargee(linges.size())
                        .build());

        machine.setEtatImmobilisation(EtatImmobilisation.EN_COURS);
        machineRepo.save(machine);
        cubixMachineEtat.demarrerLavage(machine, linges, programme.getDureeMinutes(), agent);

        bac.setEtat(EtatBac.EN_LAVAGE);
        bacRepository.save(bac);

        for (Linge linge : linges) {
            EtatLinge avant = linge.getEtat();
            linge.setEtat(EtatLinge.INFECTE_EN_LAVAGE);
            lingeRepository.save(linge);

            historiqueLingeRepository.save(
                    HistoriqueLinge.builder()
                            .linge(linge)
                            .etatAvant(avant)
                            .etatApres(EtatLinge.INFECTE_EN_LAVAGE)
                            .personnelId(agent.getId())
                            .typeOperation(TypeOperation.DEMARRAGE_LAVAGE)
                            .dateOperation(LocalDateTime.now())
                            .observations("Désinfection — bac " + bac.getCode()
                                    + " — machine " + codeMachine)
                            .build());
        }

        return cycle;
    }

    /**
     * Bac de déchargement scanné par l'agent : un bac de tri disponible (VIDE) et vide.
     */
    private Bac resoudreBacDechargement(String codeBac, List<Bac> dejaUtilises) {

        if (codeBac == null || codeBac.isBlank()) {
            throw new BusinessException(
                    "Scannez le bac de déchargement pour terminer ce cycle de désinfection.");
        }
        if (!dejaUtilises.isEmpty()) {
            throw new BusinessException(
                    "Un seul bac de déchargement est accepté pour ce cycle.");
        }

        String code = codeBac.trim();
        Bac bac = bacRepository.findByCode(code)
                .or(() -> bacRepository.findByRfidTag(code))
                .orElseThrow(() -> new ResourceNotFoundException("Bac", "code", code));

        if (bac.getUsage() != UsageBac.TRI || bac.getTypebac() == TypeBac.INFECTE) {
            throw new BusinessException(
                    "Le bac " + bac.getCode() + " n'est pas un bac de tri blanc : "
                            + "scannez un bac de déchargement.");
        }
        if (bac.getEtat() != EtatBac.VIDE) {
            throw new BusinessException(
                    "Le bac " + bac.getCode() + " n'est pas disponible (état : "
                            + bac.getEtat() + "). Scannez un bac vide.");
        }

        boolean contientDesLinges = !lingeRepository.findByBacId(bac.getId()).isEmpty();
        boolean porteDesQuantites = bac.getQuantiteDeclaree() != null && bac.getQuantiteDeclaree() > 0;
        if (contientDesLinges || porteDesQuantites) {
            throw new BusinessException(
                    "Le bac " + bac.getCode() + " n'est pas vide. Scannez un bac vide.");
        }

        dejaUtilises.add(bac);
        return bac;
    }

    private void remplirBacDechargement(Bac bac,
                                        List<Linge> linges,
                                        Map<CategLinge, Integer> quantitesDeclarees,
                                        Double poids,
                                        String source) {

        Map<CategLinge, Integer> parCategorie = new EnumMap<>(CategLinge.class);
        int total;

        if (linges != null && !linges.isEmpty()) {
            for (Linge linge : linges) {
                if (linge.getCategorie() != null) {
                    parCategorie.merge(linge.getCategorie(), 1, Integer::sum);
                }
            }
            total = linges.size();
        } else {
            if (quantitesDeclarees != null) {
                parCategorie.putAll(quantitesDeclarees);
            }
            total = parCategorie.values().stream().mapToInt(Integer::intValue).sum();
        }

        double p = poids != null ? poids : 0.0;

        if (bac.getCapacite_max() != null && p > bac.getCapacite_max()) {
            throw new BusinessException(
                    "Le poids à décharger (" + p + " kg) dépasse la capacité du bac "
                            + bac.getCode() + " (" + bac.getCapacite_max() + " kg). "
                            + "Scannez un bac plus grand.");
        }

        CategLinge dominante = parCategorie.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String detail = parCategorie.entrySet().stream()
                .map(e -> e.getKey().name() + " : " + e.getValue())
                .collect(Collectors.joining(", "));
        if (detail.length() > 300) {
            detail = detail.substring(0, 297) + "...";
        }

        bac.setCategorie(dominante);
        bac.setEtat(EtatBac.EN_ATTENTE_SECHAGE);
        bac.setPoids(p);
        bac.setPoidsBalance(p > 0 ? p : null);
        bac.setSource(source);
        bac.setQuantiteDeclaree(total);
        bac.setDetailQuantites(detail);
        bacRepository.save(bac);
    }

    /** Programme de désinfection : DESINFECTION_INFECTE, sinon le premier de type DESINFECTION. */
    private ProgrammeLavage programmeDesinfection() {
        return programmeRepo.findByNom("DESINFECTION_INFECTE")
                .or(() -> programmeRepo.findFirstByType(TypeProgramme.DESINFECTION))
                .orElseThrow(() -> new BusinessException(
                        "Aucun programme de désinfection n'est défini (type DESINFECTION)."));
    }

    /** Fin de désinfection : les linges deviennent LAVE, avec historique. */
    private void terminerLingesInfectes(List<Linge> linges, CycleLavage cycle, String origine, Long personnelIdSecours) {
        for (Linge linge : linges) {
            EtatLinge avant = linge.getEtat();
            linge.setEtat(EtatLinge.LAVE);
            linge.setNbLavage(linge.getNbLavage() + 1);
            historiqueLingeRepository.save(
                    HistoriqueLinge.builder()
                            .linge(linge)
                            .etatAvant(avant)
                            .etatApres(EtatLinge.LAVE)
                            .personnelId(cycle.getPersonnelId() != null
                                    ? cycle.getPersonnelId()
                                    : personnelIdSecours)
                            .typeOperation(TypeOperation.FIN_LAVAGE)
                            .dateOperation(LocalDateTime.now())
                            .observations("Fin de désinfection — " + origine)
                            .build());
        }
    }



}