package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.service.management.ProfilExploitationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.dto.PersonnelDto;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.service.referentiel.PersonnelReferentielService;
import tn.softsys.buanderie.dto.Lot.LotScanResponse;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.Reporting.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CycleLavageService {

    private final CycleLavageRepository cycleRepo;
    private final LotRepository lotRepo;
    private final MachineRepository machineRepo;
    private final ProgrammeLavageRepository programmeRepo;
    private final CycleLavageItemRepository itemRepo;
    private final AgentRepository agentRepository;
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final NotificationService notificationService;
    private final SacInfecteRepository sacInfecteRepository;
    private final ProfilExploitationService profilExploitationService;
    private final PersonnelReferentielService personnelReferentielService;
    private final PersonnelRepository sqlPersonnelRepo;


    public LotScanResponse scanLot(String codeLot) {
        Lot lot = lotRepo.findByCodeLot(codeLot)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", "code", codeLot));
        SuggestionProgramme suggestion = resoudreProgramme(lot.getCategorie());
        ProgrammeLavage programme = suggestion.programme();
        return LotScanResponse.builder()
                .codeLot(lot.getCodeLot())
                .categorie(lot.getCategorie())
                //.poidsRestant(lot.getPoidsRestant())
                //.quantiteRestante(lot.getQuantiteRestante())
                .programmeRecommande(programme.getNom())
                .programmeId(programme.getId())
                .temperature(programme.getTemperatureCelsius())
                .description(programme.getDescription())
                .vitesse(programme.getVitesseEssorageRpm())
                .duree(programme.getDureeMinutes())
                .origineProgramme(suggestion.parDefaut() ? "DEFAUT" : "CATEGORIE")
                .programmeParDefaut(suggestion.parDefaut())
                .build();
    }

    private record SuggestionProgramme(ProgrammeLavage programme, boolean parDefaut) {}

    /**
     * Programme de la catégorie dominante du lot ; à défaut, programme par défaut.
     */
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




    @Transactional
    public CycleLavage startCycle(String codeMachine,
                                  String codeLot,
                                  String matriculeAgent,
                                  Double poidsCharge) {
        Personnel agent = sqlPersonnelRepo.findByCodeBarre(matriculeAgent);
//                .orElseThrow(() -> new ResourceNotFoundException("AGENT", "matriculeAgent", matriculeAgent));


        Machine machine = machineRepo.findByCode(codeMachine);
        if (machine == null) {
            throw new ResourceNotFoundException("Machine", "code", codeMachine);
        }

        Lot lot = lotRepo.findByCodeLot(codeLot)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Lot", "code", codeLot
                        )
                );
//
//        if (machine.getCapaciteKg() != null
//                && poidsCharge > machine.getCapaciteKg()) {
//
//            throw new RuntimeException("Capacité machine insuffisante");
//        }

        if (machine.getEtatImmobilisation() != EtatImmobilisation.DISPONIBLE) {
            throw new RuntimeException("Machine indisponible");

        }

        if (poidsCharge == null) {
            poidsCharge = lot.getPoidsBalance() != null ? lot.getPoidsBalance() : lot.getPoidsActuel();
        }

        ProgrammeLavage programme = suggestProgramme(lot.getCategorie());
        CycleLavage cycle = CycleLavage.builder()
                .machine(machine)
                .programmeLavage(programme)
                .personnelId(agent.getId())
                .poidsCharge(poidsCharge)
                .dateDebut(LocalDateTime.now())
                .statut(StatutCycle.EN_COURS)
                .build();
        cycle = cycleRepo.save(cycle);
        List<Linge> linges = lingeRepository.findByLot_CodeLot(codeLot);

        for (Linge linge : linges) {
            EtatLinge ancienEtat = linge.getEtat();
            linge.setEtat(EtatLinge.EN_LAVAGE);
            linge.setDureVie(linge.getDureVie()-1);
            if (linge.getNbLavage() >= linge.getDureVie()) {

                notificationService.notifierFinDeVie(linge);

            }
            HistoriqueLinge historique = HistoriqueLinge.builder()
                    .linge(linge)
                    .etatAvant(ancienEtat)
                    .etatApres(EtatLinge.EN_LAVAGE)
                    .personnelId(agent.getId())
                    .typeOperation(TypeOperation.DEMARRAGE_LAVAGE)
                    .dateOperation(LocalDateTime.now())
                    .observations("Passage en lavage")
                    .dateOperation(LocalDateTime.now())
                    //.collecte(linge.getc())
                    // .sacInfecte(linge.getSacInfecte())
                    .build();
            historiqueLingeRepository.save(historique);
        }
        mettreLingesEnLavage(codeLot);
        addLotToCycleInternal(cycle, lot, poidsCharge);
        //viderLot(lot);
        machine.setEtatImmobilisation(EtatImmobilisation.EN_COURS);
        lot.setEtat(EtatLot.EN_LAVAGE);
        // lot.setPoidsActuel(0.0);
        return cycle;
    }
    public CycleLavage addLotToCycle(Long cycleId,
                                     String codeLot,
                                     Double poidsCharge) {
        CycleLavage cycle = cycleRepo.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle non trouvé"));
        Lot lot = lotRepo.findByCodeLot(codeLot)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", "code", codeLot));
        double nouveauPoids = cycle.getPoidsCharge() + poidsCharge;
//        if (nouveauPoids > cycle.getMachine().getCapaciteKg()) {
//            throw new RuntimeException("Capacité dépassée");
//        }
        List<Linge> linges  = lingeRepository.findByLot_CodeLot(codeLot);
        //mettre a jours l'etat des  linges
        mettreLingesEnLavage(codeLot);

        cycle.setPoidsCharge(nouveauPoids);
        addLotToCycleInternal(cycle, lot, poidsCharge);
        return cycleRepo.save(cycle);
    }
    private void addLotToCycleInternal(CycleLavage cycle,
                                       Lot lot,
                                       Double poidsCharge) {
        CycleLavageItem item = CycleLavageItem.builder()
                .cycleLavage(cycle)
                .typeSource(TypeSource.LOT_TRI)
                .sourceId(lot.getId())
                .poidsCharge(poidsCharge)
                .quantiteChargee(null)
                .build();
        itemRepo.save(item);
        //  mise à jour lot (consommation partielle)
        //lot.setPoidsRestant(lot.getPoidsRestant() - poidsCharge);
        lotRepo.save(lot);
    }
    @Transactional
    public CycleLavage finishCycle(Long cycleId) {

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

        for (CycleLavageItem item : items) {


            if (item.getTypeSource() == TypeSource.LOT_TRI) {

                Lot lot = lotRepo.findById(item.getSourceId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Lot introuvable"
                                ));

                List<Linge> linges =
                        lingeRepository.findByLot_CodeLot(
                                lot.getCodeLot()
                        );

                for (Linge linge : linges) {

                    EtatLinge ancienEtat = linge.getEtat();

                    linge.setEtat(EtatLinge.LAVE);
                    linge.setNbLavage(
                            linge.getNbLavage() + 1
                    );

                    historiqueLingeRepository.save(
                            HistoriqueLinge.builder()
                                    .linge(linge)
                                    .etatAvant(ancienEtat)
                                    .etatApres(EtatLinge.LAVE)
                                    .personnelId(cycle.getPersonnelId())
                                    .typeOperation(TypeOperation.FIN_LAVAGE)
                                    .dateOperation(LocalDateTime.now())
                                    .observations(
                                            "Fin du cycle de lavage"
                                    )
                                    .build()
                    );
                }

                lingeRepository.saveAll(linges);


                lot.setEtat(EtatLot.PRET_SECHAGE);
                lotRepo.save(lot);
            }

            // =====================================================
            //  LAVAGE INFECTÉ : SOURCE = SAC_INFECTE
            // =====================================================
            else if (item.getTypeSource() == TypeSource.SAC_INFECTE) {

                SacInfecte sac = sacInfecteRepository
                        .findById(item.getSourceId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Sac infecté introuvable"
                                ));



                // jusqu'à la vérification physique.
                if (sac.getEtat() != EtatSac.EN_LAVAGE) {
                    throw new BusinessException(
                            "Le sac " + sac.getNumeroSac()
                                    + " n'est pas EN_LAVAGE."
                    );
                }

                sacInfecteRepository.save(sac);
            }
        }

        machine.setEtatImmobilisation(EtatImmobilisation.DISPONIBLE);
        machineRepo.save(machine);

        return cycleRepo.save(cycle);
    }



    //METHODE POUR METTRE A JOURS LES ETATS ES LINGES EN LAVAGE
    private void mettreLingesEnLavage(String codeLot) {
        List<Linge> linges = lingeRepository.findByLot_CodeLot(codeLot);

        for (Linge linge : linges) {
            linge.setEtat(EtatLinge.EN_LAVAGE);
        }
        lingeRepository.saveAll(linges);
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

    //methode pour vider le lots mis en lavage
    @Transactional
    public void viderLot(Lot lot) {
        List<Linge> linges =
                lingeRepository.findByLot_CodeLot(lot.getCodeLot());
        for(Linge linge : linges) {
            linge.setLot(null);
            lingeRepository.save(linge);
        }
        lot.setPoidsActuel(0.0);
        lot.setEtat(EtatLot.EN_LAVAGE);
        lotRepo.save(lot);
    }
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

        for (CycleLavageItem item : items) {
            if (item.getSourceId() == null) continue;

            List<Linge> linges = lingeRepository.findByLot_Id(item.getSourceId());

            for (Linge l : linges) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("lingeId",   l.getId());
                m.put("codeBarre", l.getCodeBarre());
                m.put("rfidTag",   l.getRfidTag());
                m.put("categorie", l.getCategorie() != null ? l.getCategorie().name() : null);
                m.put("etat",      l.getEtat() != null ? l.getEtat().name() : null);
                m.put("poids",     l.getPoids());
                m.put("lotId",     item.getSourceId());
                m.put("itemId",    item.getId());
                result.add(m);
            }
        }
        return result;
    }




    //===========================================================//


    @Transactional
    public CycleLavage startCycleSacInfecte(
            String codeMachine,
            String numeroSac,
            String matriculeAgent) {
        profilExploitationService.exigerCircuitInfecte();


        // Agent
        // Agent agent = agentRepository.findByMatricule(matriculeAgent)
        PersonnelDto agent = personnelReferentielService.getByCode(matriculeAgent);

//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "AGENT",
//                                "matriculeAgent",
//                                matriculeAgent
//                        ));

        // Sac
        SacInfecte sac = sacInfecteRepository
                .findByNumeroSac(numeroSac)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "SacInfecte",
                                "numeroSac",
                                numeroSac
                        ));

        // Vérification état
        if (sac.getEtat() != EtatSac.RECEPTIONNE) {
            throw new BusinessException(
                    "Le sac doit être RECEPTIONNE. État actuel : "
                            + sac.getEtat()
            );
        }

        // Machine
        Machine machine = machineRepo.findByCode(codeMachine);

        if (machine == null) {
            throw new ResourceNotFoundException(
                    "Machine",
                    "code",
                    codeMachine
            );
        }

        if (machine.getEtatImmobilisation() != EtatImmobilisation.DISPONIBLE) {
            throw new BusinessException(
                    "La machine " + codeMachine +
                            " n'est pas disponible."
            );
        }

        // Programme automatique de désinfection
        ProgrammeLavage programmeDesinfection =
                programmeRepo.findByNom("DESINFECTION_INFECTE")
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Le programme DESINFECTION_INFECTE " +
                                                "est introuvable."
                                ));

        // Création du cycle
        CycleLavage cycle = CycleLavage.builder()
                .machine(machine)
                //.agent(agent)
                .programmeLavage(programmeDesinfection)
                .dateDebut(LocalDateTime.now())
                .statut(StatutCycle.EN_COURS)
                .poidsCharge(0.0)
                .build();

        cycle = cycleRepo.save(cycle);

        // Le cycle contient le SAC, pas les linges
        CycleLavageItem item = CycleLavageItem.builder()
                .cycleLavage(cycle)
                .typeSource(TypeSource.SAC_INFECTE)
                .sourceId(sac.getId())
                .quantiteChargee(sac.getQuantiteDeclaree())
                .build();

        itemRepo.save(item);

        // Machine occupée
        machine.setEtatImmobilisation(EtatImmobilisation.EN_COURS);
        machineRepo.save(machine);

        // Sac en lavage
        sac.setEtat(EtatSac.EN_LAVAGE);
        sac.setDateLavage(LocalDateTime.now());
        //  sac.setAgentLavage(agent);
        sac.setCycleLavage(cycle);

        sacInfecteRepository.save(sac);
        for (Linge linge : sac.getLinges()) {

            EtatLinge avant = linge.getEtat();

            linge.setEtat(EtatLinge.INFECTE_EN_LAVAGE);
            lingeRepository.save(linge);

            historiqueLingeRepository.save(
                    HistoriqueLinge.builder()
                            .linge(linge)
                            .etatAvant(avant)
                            .etatApres(EtatLinge.INFECTE_EN_LAVAGE)
                            .personnelId(agent != null ? agent.getId() : sac.getPersonnelId())
                            .typeOperation(TypeOperation.LAVAGE_SAC_INFECTE)
                            .dateOperation(LocalDateTime.now())
                            .sacInfecte(sac)
                            .collecte(sac.getCollecte())
                            .observations("Mise en lavage — sac " + sac.getNumeroSac()
                                    + " — machine " + codeMachine)
                            .build()
            );
        }

        return cycle;
    }
}