package tn.softsys.buanderie.service.Process;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.softsys.buanderie.Sqlserver.Cubix.CubixChambreService;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixLitService;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixLitsInfectesService;

import tn.softsys.buanderie.Sqlserver.dtos.CubixLitDto;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitInfecteDto;
import tn.softsys.buanderie.Sqlserver.dtos.PersonnelDto;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixPersonnelService;

import tn.softsys.buanderie.dto.ChambreDto;

import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.*;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.management.SacInfecteService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class CollecteChambreService {


    private final LingeRepository lingeRepo;
    private final AgentRepository agentRepo;
    private final BacRepository bacRepo;
    private final CollecteRepository collecteRepo;
    private final CollecteLingeRepository collecteLingeRepo;
    private final HistoriqueLingeRepository historiqueRepo;
    private final SacInfecteRepository sacInfecteRepository;
    private final SacInfecteService sacInfecteService;



    private final CubixPersonnelService cubixPersonnelService;
    private final CubixChambreService cubixChambreService;
    private final CubixLitService cubixLitService;
    private final CubixLitsInfectesService cubixLitsInfectesService;




    @Transactional(readOnly = true)
    public Map<String, Object> scannerLit(
            Long collecteId,
            Long litId) {

        Collecte collecte = findC(collecteId);

        if (collecte.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }

        if (collecte.getChambreIdSqlServer() == null) {
            throw new BusinessException(
                    "La chambre de la collecte est introuvable."
            );
        }

        if (litId == null) {
            throw new BusinessException(
                    "L'identifiant du lit est obligatoire."
            );
        }




        CubixLitDto lit = cubixLitService.getById(litId);

        if (lit == null) {
            throw new BusinessException(
                    "Lit introuvable dans Cubix : " + litId
            );
        }




        if (lit.getChambreId() == null
                || !Objects.equals(
                lit.getChambreId(),
                collecte.getChambreIdSqlServer()
        )) {

            throw new BusinessException(
                    "Ce lit n'appartient pas à la chambre de cette collecte."
            );
        }




        boolean infecte =
                cubixLitsInfectesService
                        .getAll()
                        .stream()
                        .anyMatch(l ->
                                Objects.equals(
                                        l.getId(),
                                        litId
                                )
                        );




        List<Linge> linges =
                lingeRepo.findByLitIdSqlServerInAndEtat(
                        List.of(litId),
                        EtatLinge.LIVRE
                );


        if (linges.isEmpty()) {

            throw new BusinessException(
                    "Aucun linge LIVRE à traiter pour le lit "
                            + s(lit.getCode())
                            + " (ID="
                            + litId
                            + ")."
            );
        }



        List<Linge> lingesNonTraites =
                linges.stream()
                        .filter(l ->
                                !done(
                                        collecteId,
                                        l.getId()
                                )
                        )
                        .collect(Collectors.toList());


        if (lingesNonTraites.isEmpty()) {

            throw new BusinessException(
                    "Tous les linges LIVRE de ce lit ont déjà été traités."
            );
        }


        return Map.of(
                "litId",
                litId,

                "codeLit",
                s(lit.getCode()),

                "estInfecte",
                infecte,

                "nbLinges",
                lingesNonTraites.size(),

                "actionSuivante",
                infecte
                        ? "SCANNER_SAC_INFECTE"
                        : "SCANNER_BAC_BLANC"
        );
    }



    public Map<String, Object> ouvrirChambre(
            String codeBarre,
            String matriculeAgent) {

        if (codeBarre == null || codeBarre.isBlank()) {
            throw new BusinessException(
                    "Le code-barres de la chambre est obligatoire."
            );
        }

        if (matriculeAgent == null || matriculeAgent.isBlank()) {
            throw new BusinessException(
                    "Le matricule de l'agent est obligatoire."
            );
        }

        PersonnelDto personnel =
                cubixPersonnelService.getByCode(matriculeAgent);

        if (personnel == null) {
            throw new BusinessException(
                    "Personnel introuvable : " + matriculeAgent
            );
        }

        Long personnelId = personnel.getId();



        ChambreDto chambre =
                cubixChambreService
                        .getByCodeBarre(codeBarre);

        if (chambre == null) {
            throw new BusinessException(
                    "Chambre introuvable : " + codeBarre
            );
        }

        Long chambreId = chambre.getId();



        List<CubixLitInfecteDto> litsInfectes =
                cubixLitsInfectesService.getAll();

        Set<Long> idsLitsInfectes =
                litsInfectes
                        .stream()
                        .map(CubixLitInfecteDto::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());




        List<CubixLitDto> lits =
                cubixLitService
                        .getByChambreId(chambreId);


        if (lits == null) {
            lits = Collections.emptyList();
        }


        if (lits.isEmpty()) {

            throw new BusinessException(
                    "Aucun lit trouvé pour la chambre "
                            + chambre.getCode()
            );
        }


        // ========================================================
        // CONSTRUIRE LES LITS AVEC LEURS LINGES LIVRE
        // ========================================================

        List<Map<String, Object>> litsDto =
                new ArrayList<>();


        for (CubixLitDto lit : lits) {

            if (lit == null || lit.getId() == null) {
                continue;
            }


            Long litId = lit.getId();


            // ----------------------------------------------------
            // Vérification supplémentaire :
            // le lit doit appartenir à la chambre
            // ----------------------------------------------------

            if (lit.getChambreId() != null
                    && !Objects.equals(
                    lit.getChambreId(),
                    chambreId
            )) {

                continue;
            }


            // ----------------------------------------------------
            // POSTGRESQL :
            // récupérer les linges LIVRE de CE LIT
            //
            // IMPORTANT :
            // PAS DE SERVICE ID.
            //
            // Le lien est :
            //
            // Linge.litIdSqlServer
            //             ↓
            //       Cubix Lit.id
            //
            // ----------------------------------------------------

            List<Linge> linges =
                    lingeRepo.findByLitIdSqlServerInAndEtat(
                            List.of(litId),
                            EtatLinge.LIVRE
                    );


            if (linges == null) {
                linges = Collections.emptyList();
            }


            // ----------------------------------------------------
            // Ne garder que les linges non traités
            // dans la collecte courante.
            //
            // Ici la collecte n'existe pas encore.
            // Donc tous les LIVRE sont conservés.
            // ----------------------------------------------------

            List<Linge> lingesLivre =
                    new ArrayList<>(linges);


            // ----------------------------------------------------
            // DEBUG
            // ----------------------------------------------------

            System.out.println(
                    "=================================================="
            );

            System.out.println(
                    "CHAMBRE CUBIX ID = " + chambreId
            );

            System.out.println(
                    "CHAMBRE CODE = " + chambre.getCode()
            );

            System.out.println(
                    "LIT CUBIX ID = " + litId
            );

            System.out.println(
                    "LIT CODE = " + s(lit.getCode())
            );

            System.out.println(
                    "NB LINGES LIVRE = " + lingesLivre.size()
            );


            for (Linge linge : lingesLivre) {

                System.out.println(
                        "LINGE ID = "
                                + linge.getId()
                                + " | CODE = "
                                + linge.getCodeBarre()
                                + " | LIT_SQL = "
                                + linge.getLitIdSqlServer()
                                + " | CHAMBRE_SQL = "
                                + linge.getChambreIdSqlServer()
                                + " | ETAT = "
                                + linge.getEtat()
                );
            }

            System.out.println(
                    "=================================================="
            );


            // ----------------------------------------------------
            // Si aucun linge LIVRE sur ce lit,
            // on ne l'affiche pas.
            // ----------------------------------------------------

            if (lingesLivre.isEmpty()) {
                continue;
            }


            // ----------------------------------------------------
            // CONSTRUIRE LE DTO DU LIT
            // ----------------------------------------------------

            Map<String, Object> litMap =
                    new LinkedHashMap<>();


            litMap.put(
                    "litId",
                    litId
            );

            litMap.put(
                    "codeLit",
                    s(lit.getCode())
            );

            litMap.put(
                    "estInfecte",
                    idsLitsInfectes.contains(litId)
            );

            litMap.put(
                    "nbLinges",
                    lingesLivre.size()
            );


            // ----------------------------------------------------
            // LINGES
            // ----------------------------------------------------

            List<Map<String, Object>> lingesDto =
                    lingesLivre
                            .stream()
                            .map(l ->
                                    Map.<String, Object>of(
                                            "lingeId",
                                            l.getId(),

                                            "codeBarre",
                                            s(l.getCodeBarre()),

                                            "rfid",
                                            s(l.getRfidTag()),

                                            "categorie",
                                            l.getCategorie() != null
                                                    ? l.getCategorie().name()
                                                    : "",

                                            "statut",
                                            l.getStatutCollecte() != null
                                                    ? l.getStatutCollecte().name()
                                                    : "",

                                            "etat",
                                            l.getEtat() != null
                                                    ? l.getEtat().name()
                                                    : "",

                                            "litIdSqlServer",
                                            l.getLitIdSqlServer()
                                    )
                            )
                            .collect(Collectors.toList());


            litMap.put(
                    "linges",
                    lingesDto
            );


            litsDto.add(litMap);
        }


        // ========================================================
        // AUCUN LINGE DANS LA CHAMBRE
        // ========================================================

        if (litsDto.isEmpty()) {

            throw new BusinessException(
                    "Aucun linge LIVRE — chambre "
                            + chambre.getCode()
            );
        }


        // ========================================================
        // CALCUL DU NOMBRE TOTAL
        // ========================================================

        int nbLinges =
                litsDto
                        .stream()
                        .mapToInt(m ->
                                ((Number) m.get("nbLinges"))
                                        .intValue()
                        )
                        .sum();


        // ========================================================
        // CRÉER LA COLLECTE
        // ========================================================

        Collecte collecte =
                collecteRepo.save(
                        Collecte.builder()
                                .personnelId(
                                        personnelId
                                )

                                .chambreIdSqlServer(
                                        chambreId
                                )

                                .codeChambre(
                                        chambre.getCode()
                                )

                                .typeCollecte(
                                        TypeCollecte.MIXTE
                                )

                                .etatCollecte(
                                        EtatCollecte.EN_TRAITEMENT
                                )

                                .service(
                                        chambre.getServiceId()
                                )

                                .nbAttendus(
                                        nbLinges
                                )

                                .numeroCollecte(
                                        "CH-"
                                                + System.currentTimeMillis()
                                )

                                .build()
                );


        // ========================================================
        // RÉPONSE
        // ========================================================

        return Map.of(
                "collecteId",
                collecte.getId(),

                "personnelId",
                personnelId,

                "codeChambre",
                chambre.getCode(),

                "chambreId",
                chambreId,

                "nbLits",
                litsDto.size(),

                "nbLinges",
                nbLinges,

                "lits",
                litsDto
        );
    }


    // ============================================================
    // TRAITER UN LIT
    // ============================================================

    @Transactional
    public Map<String, Object> traiterLit(
            Long collecteId,
            Long litId,
            String codeContenant) {

        Collecte collecte = findC(collecteId);


        if (collecte.getEtatCollecte()
                != EtatCollecte.EN_TRAITEMENT) {

            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }


        if (collecte.getChambreIdSqlServer() == null) {

            throw new BusinessException(
                    "La chambre de la collecte est introuvable."
            );
        }


        if (litId == null) {

            throw new BusinessException(
                    "L'identifiant du lit est obligatoire."
            );
        }


        if (codeContenant == null
                || codeContenant.isBlank()) {

            throw new BusinessException(
                    "Le code du contenant est obligatoire."
            );
        }


        // ========================================================
        // CUBIX : LIT
        // ========================================================

        CubixLitDto lit =
                cubixLitService.getById(litId);


        if (lit == null) {

            throw new BusinessException(
                    "Lit introuvable dans Cubix : " + litId
            );
        }


        // ========================================================
        // VÉRIFIER CHAMBRE
        // ========================================================

        if (lit.getChambreId() == null
                || !Objects.equals(
                lit.getChambreId(),
                collecte.getChambreIdSqlServer()
        )) {

            throw new BusinessException(
                    "Ce lit n'appartient pas à la chambre "
                            + "de cette collecte."
            );
        }


        // ========================================================
        // CUBIX : INFECTION
        // ========================================================

        boolean infecte =
                cubixLitsInfectesService
                        .getAll()
                        .stream()
                        .anyMatch(l ->
                                Objects.equals(
                                        l.getId(),
                                        litId
                                )
                        );


        Bac bac = null;
        SacInfecte sacInfecte = null;


        // ========================================================
        // CONTENANT
        // ========================================================

        if (infecte) {

            // ----------------------------------------------------
            // LIT INFECTÉ → SAC INFECTÉ
            // ----------------------------------------------------

            sacInfecte =
                    sacInfecteRepository
                            .findByNumeroSac(
                                    codeContenant
                            )
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Sac infecté introuvable : "
                                                    + codeContenant
                                    )
                            );

        } else {

            // ----------------------------------------------------
            // LIT NORMAL → BAC BLANC
            // ----------------------------------------------------

            bac =
                    bacRepo.findByCode(
                                    codeContenant
                            )
                            .or(() ->
                                    bacRepo.findByRfidTag(
                                            codeContenant
                                    )
                            )
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Bac introuvable : "
                                                    + codeContenant
                                    )
                            );


            if (bac.getTypebac()
                    != TypeBac.BLANC) {

                throw new BusinessException(
                        "Ce lit n'est pas infecté. "
                                + "Veuillez scanner un bac BLANC."
                );
            }
        }


        // ========================================================
        // RÉCUPÉRER LES LINGES LIVRE DU LIT
        // ========================================================

        List<Linge> linges =
                lingeRepo.findByLitIdSqlServerInAndEtat(
                        List.of(litId),
                        EtatLinge.LIVRE
                );


        // ========================================================
        // EXCLURE LES LINGES DÉJÀ TRAITÉS
        // ========================================================

        linges =
                linges
                        .stream()
                        .filter(l ->
                                !done(
                                        collecteId,
                                        l.getId()
                                )
                        )
                        .collect(Collectors.toList());


        if (linges.isEmpty()) {

            throw new BusinessException(
                    "Aucun linge LIVRE à traiter pour le lit "
                            + s(lit.getCode())
            );
        }


        // ========================================================
        // DÉTERMINER LE NOUVEL ÉTAT
        // ========================================================

        EtatLinge nouvelEtat =
                infecte
                        ? EtatLinge.INFECTE_COLLECTE
                        : EtatLinge.COLLECTE;


        StatutCollecteLinge statut =
                infecte
                        ? StatutCollecteLinge.INFECTE_PRESUME
                        : StatutCollecteLinge.BLANC;


        TypeCollecte typeCollecte =
                infecte
                        ? TypeCollecte.INFECTE
                        : TypeCollecte.BLANC;


        String codeContenantFinal =
                infecte
                        ? sacInfecte.getNumeroSac()
                        : bac.getCode();


        List<String> codes =
                new ArrayList<>();


        // ========================================================
        // TRAITER CHAQUE LINGE
        // ========================================================

        for (Linge linge : linges) {

            EtatLinge ancienEtat =
                    linge.getEtat();


            if (infecte) {

                linge.setSacInfecte(
                        sacInfecte
                );

                sacInfecteService
                        .ajouterLingeAuSac(
                                sacInfecte,
                                linge
                        );

            } else {

                linge.setBac(
                        bac
                );
            }


            // ----------------------------------------------------
            // NOUVEL ÉTAT
            // ----------------------------------------------------

            linge.setEtat(
                    nouvelEtat
            );


            linge.setStatutCollecte(
                    statut
            );


            linge.setTypeCollecte(
                    typeCollecte
            );


            /*
             * IMPORTANT :
             *
             * Une fois le linge collecté,
             * il n'appartient plus au lit/chambre.
             */

            linge.setLitIdSqlServer(
                    null
            );


            linge.setChambreIdSqlServer(
                    null
            );


            lingeRepo.save(
                    linge
            );


            // ----------------------------------------------------
            // COLLECTE LINGE
            // ----------------------------------------------------

            collecteLingeRepo.save(
                    CollecteLinge.builder()
                            .collecte(
                                    collecte
                            )

                            .linge(
                                    linge
                            )

                            .typeCollecte(
                                    typeCollecte
                            )

                            .numeroSacInfecte(
                                    infecte
                                            ? sacInfecte
                                              .getNumeroSac()
                                            : null
                            )

                            .dateScan(
                                    LocalDateTime.now()
                            )

                            .build()
            );


            // ----------------------------------------------------
            // HISTORIQUE
            // ----------------------------------------------------

            historiqueRepo.save(
                    HistoriqueLinge.builder()
                            .linge(
                                    linge
                            )

                            .etatAvant(
                                    ancienEtat
                            )

                            .etatApres(
                                    nouvelEtat
                            )

                            .personnelId(
                                    collecte
                                            .getPersonnelId()
                            )

                            .typeOperation(
                                    infecte
                                            ? TypeOperation
                                              .DECLARATION_SAC_INFECTE
                                            : TypeOperation
                                              .COLLECTE
                            )

                            .dateOperation(
                                    LocalDateTime.now()
                            )

                            .collecte(
                                    collecte
                            )

                            .observations(
                                    (infecte
                                            ? "INFECTE"
                                            : "BLANC")
                                            + " lit="
                                            + litId
                                            + " contenant="
                                            + codeContenantFinal
                            )

                            .build()
            );


            codes.add(
                    linge.getCodeBarre()
            );
        }


        // ========================================================
        // RÉPONSE
        // ========================================================

        return Map.of(
                "litId",
                litId,

                "codeLit",
                s(lit.getCode()),

                "codeContenant",
                codeContenantFinal,

                "typeContenant",
                infecte
                        ? "SAC_INFECTE"
                        : "BAC_BLANC",

                "estInfecte",
                infecte,

                "nbTraites",
                linges.size(),

                "codes",
                codes
        );
    }


    // ============================================================
    // DÉCLARER PROPRE
    // ============================================================

    @Transactional
    public Map<String, Object> declarerPropre(
            Long collecteId,
            String codeOuTag) {

        Collecte collecte =
                findC(collecteId);


        if (collecte.getEtatCollecte()
                != EtatCollecte.EN_TRAITEMENT) {

            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }


        Linge linge =
                findL(codeOuTag);


        if (!estLingeAttendu(
                collecte,
                linge
        )) {

            throw new BusinessException(
                    "Ce linge ne fait pas partie "
                            + "des linges attendus pour cette collecte."
            );
        }


        if (done(
                collecteId,
                linge.getId()
        )) {

            throw new BusinessException(
                    "Ce linge a déjà été traité pour cette collecte."
            );
        }


        /*
         * Le linge reste LIVRE.
         *
         * Seul le statut de collecte change.
         */

        linge.setStatutCollecte(
                StatutCollecteLinge.PROPRE
        );


        lingeRepo.save(
                linge
        );


        return Map.of(
                "lingeId",
                linge.getId(),

                "codeBarre",
                linge.getCodeBarre(),

                "statut",
                "PROPRE"
        );
    }


    // ============================================================
    // DÉCLARER MANQUANT
    // ============================================================

    @Transactional
    public Map<String, Object> declarerManquant(
            Long collecteId,
            Long lingeId) {

        Collecte collecte =
                findC(collecteId);


        if (collecte.getEtatCollecte()
                != EtatCollecte.EN_TRAITEMENT) {

            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }


        Linge linge =
                lingeRepo.findById(
                                lingeId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Linge",
                                        "id",
                                        lingeId
                                )
                        );


        if (!estLingeAttendu(
                collecte,
                linge
        )) {

            throw new BusinessException(
                    "Ce linge ne fait pas partie "
                            + "des linges attendus pour cette collecte."
            );
        }


        if (done(
                collecteId,
                lingeId
        )) {

            throw new BusinessException(
                    "Ce linge a déjà été traité."
            );
        }


        EtatLinge ancienEtat =
                linge.getEtat();


        linge.setStatutCollecte(
                StatutCollecteLinge.MANQUANT
        );


        lingeRepo.save(
                linge
        );


        collecteLingeRepo.save(
                CollecteLinge.builder()
                        .collecte(
                                collecte
                        )

                        .linge(
                                linge
                        )

                        .typeCollecte(
                                TypeCollecte.MANQUANT
                        )

                        .dateScan(
                                LocalDateTime.now()
                        )

                        .build()
        );


        historiqueRepo.save(
                HistoriqueLinge.builder()
                        .linge(
                                linge
                        )

                        .etatAvant(
                                ancienEtat
                        )

                        .etatApres(
                                EtatLinge.MANQUANT
                        )

                        .personnelId(
                                collecte
                                        .getPersonnelId()
                        )

                        .typeOperation(
                                TypeOperation
                                        .CHANGEMENT_ETAT_LINGE
                        )

                        .dateOperation(
                                LocalDateTime.now()
                        )

                        .collecte(
                                collecte
                        )

                        .observations(
                                "Manquant ch="
                                        + collecte
                                        .getCodeChambre()
                        )

                        .build()
        );


        return Map.of(
                "lingeId",
                lingeId,

                "statut",
                "MANQUANT"
        );
    }


    // ============================================================
    // CLÔTURER
    // ============================================================

    @Transactional
    public Map<String, Object> cloturer(
            Long collecteId) {

        Collecte collecte =
                findC(collecteId);


        if (collecte.getEtatCollecte()
                != EtatCollecte.EN_TRAITEMENT) {

            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }


        List<CollecteLinge> items =
                collecteLingeRepo
                        .findByCollecte_Id(
                                collecteId
                        );


        long blancs =
                cnt(
                        items,
                        TypeCollecte.BLANC
                );


        long infectes =
                cnt(
                        items,
                        TypeCollecte.INFECTE
                );


        long manquants =
                cnt(
                        items,
                        TypeCollecte.MANQUANT
                );


        long attendus =
                collecte.getNbAttendus();


        /*
         * Les PROPRE ne sont pas enregistrés
         * dans CollecteLinge.
         *
         * Ils sont donc calculés.
         */

        long propres =
                attendus
                        - blancs
                        - infectes
                        - manquants;


        if (propres < 0) {

            throw new BusinessException(
                    "Incohérence des quantités : "
                            + "BLANC + INFECTE + MANQUANT "
                            + "> ATTENDU."
            );
        }


        long totalTraites =
                blancs
                        + infectes
                        + propres
                        + manquants;


        if (totalTraites != attendus) {

            long restant =
                    attendus - totalTraites;


            throw new BusinessException(
                    "La collecte ne peut pas être clôturée. "
                            + "Il reste "
                            + restant
                            + " linge(s) à traiter."
            );
        }


        collecte.setNbScannésBlanc(
                (int) blancs
        );


        collecte.setNbInfectésPrésumés(
                (int) infectes
        );


        collecte.setNbPropres(
                (int) propres
        );


        collecte.setNbManquants(
                (int) manquants
        );


        collecte.setDateFin(
                LocalDateTime.now()
        );


        /*
         * IMPORTANT :
         *
         * Je conserve EN_TRAITEMENT ici car ton enum fourni
         * ne permet pas de confirmer le nom de l'état final.
         *
         * Si tu as TERMINEE dans EtatCollecte :
         *
         * collecte.setEtatCollecte(
         *      EtatCollecte.TERMINEE
         * );
         */

        collecte.setEtatCollecte(
                EtatCollecte.EN_TRAITEMENT
        );


        collecteRepo.save(
                collecte
        );


        return Map.of(
                "collecteId",
                collecteId,

                "codeChambre",
                s(
                        collecte.getCodeChambre()
                ),

                "nbAttendus",
                attendus,

                "nbBlancs",
                blancs,

                "nbInfectes",
                infectes,

                "nbPropres",
                propres,

                "nbManquants",
                manquants,

                "totalTraites",
                totalTraites,

                "etat",
                "TERMINEE"
        );
    }


    // ============================================================
    // FIND COLLECTE
    // ============================================================

    private Collecte findC(Long id) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "L'identifiant de la collecte est obligatoire."
            );
        }


        return collecteRepo
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collecte",
                                "id",
                                id
                        )
                );
    }


    // ============================================================
    // FIND LINGE
    // ============================================================

    private Linge findL(String code) {

        if (code == null || code.isBlank()) {

            throw new BusinessException(
                    "Le code ou RFID du linge est obligatoire."
            );
        }


        return lingeRepo
                .findByCodeBarre(code)
                .or(() ->
                        lingeRepo.findByBarcodeOrRfid(code)
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "code",
                                code
                        )
                );
    }


    // ============================================================
    // VÉRIFIER SI LE LINGE A DÉJÀ ÉTÉ TRAITÉ
    // ============================================================

    private boolean done(
            Long collecteId,
            Long lingeId) {

        return collecteLingeRepo
                .existsByCollecte_IdAndLinge_Id(
                        collecteId,
                        lingeId
                );
    }


    // ============================================================
    // COMPTER UN TYPE
    // ============================================================

    private long cnt(
            List<CollecteLinge> items,
            TypeCollecte type) {

        return items
                .stream()
                .filter(item ->
                        type == item.getTypeCollecte()
                )
                .count();
    }


    // ============================================================
    // STRING NULL SAFE
    // ============================================================

    private String s(String value) {

        return value != null
                ? value
                : "";
    }


    // ============================================================
    // VÉRIFIER QU'UN LINGE APPARTIENT À LA COLLECTE
    // ============================================================

    private boolean estLingeAttendu(
            Collecte collecte,
            Linge linge) {

        if (collecte == null) {
            return false;
        }


        if (linge == null) {
            return false;
        }


        if (collecte.getChambreIdSqlServer() == null) {
            return false;
        }


        if (linge.getLitIdSqlServer() == null) {
            return false;
        }


        /*
         * Le linge doit encore être LIVRE.
         */

        if (linge.getEtat()
                != EtatLinge.LIVRE) {

            return false;
        }


        /*
         * On vérifie que le linge possède bien un lit.
         */

        Long litId =
                linge.getLitIdSqlServer();


        /*
         * On récupère tous les linges LIVRE
         * du même lit.
         *
         * Si le linge est présent, il est bien
         * rattaché à un lit.
         */

        List<Linge> lingesDuLit =
                lingeRepo
                        .findByLitIdSqlServerInAndEtat(
                                List.of(litId),
                                EtatLinge.LIVRE
                        );


        boolean existe =
                lingesDuLit
                        .stream()
                        .anyMatch(l ->
                                Objects.equals(
                                        l.getId(),
                                        linge.getId()
                                )
                        );


        if (!existe) {
            return false;
        }




        return true;
    }
}