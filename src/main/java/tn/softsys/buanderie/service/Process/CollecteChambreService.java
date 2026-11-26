package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.entity.enums.NiveauRattachement;

import tn.softsys.buanderie.service.management.ProfilExploitationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import tn.softsys.buanderie.service.referentiel.PersonnelReferentielService;
import tn.softsys.buanderie.dto.PersonnelDto;

import tn.softsys.buanderie.dto.ChambreDto;
import tn.softsys.buanderie.dto.LitDto;
import tn.softsys.buanderie.dto.LitInfecteDto;
import tn.softsys.buanderie.service.referentiel.ChambreLitReferentielService;

import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.*;
import tn.softsys.buanderie.repository.*;

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



    private final PersonnelReferentielService personnelService;
    private final ChambreLitReferentielService chambreLitService;
    private final ProfilExploitationService profilExploitationService;




    @Transactional(readOnly = true)
    public Map<String, Object> scannerLit(
            Long collecteId,
            Long litId) {
        profilExploitationService.exigerNiveau(NiveauRattachement.LIT);


        Collecte collecte = findC(collecteId);

        if (collecte.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }

        if (collecte.getChambreId() == null) {
            throw new BusinessException(
                    "La chambre de la collecte est introuvable."
            );
        }

        if (litId == null) {
            throw new BusinessException(
                    "L'identifiant du lit est obligatoire."
            );
        }




        LitDto lit = chambreLitService.getLitById(litId);

        if (lit == null) {
            throw new BusinessException(
                    "Lit introuvable : " + litId
            );
        }




        if (lit.getChambreId() == null
                || !Objects.equals(
                lit.getChambreId(),
                collecte.getChambreId()
        )) {

            throw new BusinessException(
                    "Ce lit n'appartient pas à la chambre de cette collecte."
            );
        }




        boolean infecte =
                chambreLitService
                        .getLitsInfectes()
                        .stream()
                        .anyMatch(l ->
                                Objects.equals(
                                        l.getId(),
                                        litId
                                )
                        );




        List<Linge> linges =
                lingeRepo.findByLitIdInAndEtat(
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
        profilExploitationService.exigerNiveau(NiveauRattachement.LIT);


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
                personnelService.getByCode(matriculeAgent);

        if (personnel == null) {
            throw new BusinessException(
                    "Personnel introuvable : " + matriculeAgent
            );
        }

        Long personnelId = personnel.getId();



        ChambreDto chambre =
                chambreLitService
                        .getChambreByCodeBarre(codeBarre);

        if (chambre == null) {
            throw new BusinessException(
                    "Chambre introuvable : " + codeBarre
            );
        }

        Long chambreId = chambre.getId();



        List<LitInfecteDto> litsInfectes =
                chambreLitService.getLitsInfectes();

        Set<Long> idsLitsInfectes =
                litsInfectes
                        .stream()
                        .map(LitInfecteDto::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());




        List<LitDto> lits =
                chambreLitService
                        .getLitsByChambre(chambreId);


        if (lits == null) {
            lits = Collections.emptyList();
        }


        // une chambre sans lit reste collectable (linges de chambre)


        // ========================================================
        // CONSTRUIRE LES LITS AVEC LEURS LINGES LIVRE
        // ========================================================

        List<Map<String, Object>> litsDto =
                new ArrayList<>();


        for (LitDto lit : lits) {

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
            // Linge.litId
            //             ↓
            //       Cubix Lit.id
            //
            // ----------------------------------------------------

            List<Linge> linges =
                    lingeRepo.findByLitIdInAndEtat(
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
                                + linge.getLitId()
                                + " | CHAMBRE_SQL = "
                                + linge.getChambreId()
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

                                            "litId",
                                            l.getLitId()
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
        // LINGES DE CHAMBRE (sans lit) : LIVRE avec chambreId, litId null
        // ========================================================

        List<Linge> lingesChambre = lingesDeChambre(chambreId);

        Map<String, Object> chambreMap = new LinkedHashMap<>();
        chambreMap.put("nbLinges", 0);
        chambreMap.put("linges", new ArrayList<Object>());
        if (!lingesChambre.isEmpty()) {
            chambreMap.put("nbLinges", lingesChambre.size());
            chambreMap.put("actionSuivante", "SCANNER_BAC");
            chambreMap.put("linges", lingesChambre.stream()
                    .map(l -> Map.<String, Object>of(
                            "lingeId", l.getId(),
                            "codeBarre", s(l.getCodeBarre()),
                            "rfid", s(l.getRfidTag()),
                            "categorie", l.getCategorie() != null ? l.getCategorie().name() : "",
                            "etat", l.getEtat() != null ? l.getEtat().name() : ""))
                    .collect(Collectors.toList()));
        }

        // ========================================================
        // AUCUN LINGE DANS LA CHAMBRE
        // ========================================================

        if (litsDto.isEmpty() && lingesChambre.isEmpty()) {

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
        nbLinges += lingesChambre.size();

        Collecte collecte =
                collecteRepo.save(
                        Collecte.builder()
                                .personnelId(
                                        personnelId
                                )

                                .chambreId(
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
                litsDto,

                "lingesChambre",
                chambreMap
        );
    }

    /** Linges LIVRE posés sur la chambre elle-même (sans lit). */
    private List<Linge> lingesDeChambre(Long chambreId) {
        List<Linge> l = lingeRepo.findByChambreIdAndEtat(chambreId, EtatLinge.LIVRE);
        if (l == null) {
            return Collections.emptyList();
        }
        return l.stream().filter(x -> x.getLitId() == null).collect(Collectors.toList());
    }

    /**
     * Collecte des linges de chambre (sans lit) : l'agent scanne le bac (ou le sac infecté)
     * puis les linges ; si lingeIds est vide, tous les linges de chambre restants sont pris.
     */
    @Transactional
    public Map<String, Object> traiterLingesChambre(
            Long collecteId,
            String codeContenant,
            List<Long> lingeIds) {

        Collecte collecte = findC(collecteId);

        if (collecte.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException("La collecte n'est plus en cours de traitement.");
        }
        if (collecte.getChambreId() == null) {
            throw new BusinessException("La chambre de la collecte est introuvable.");
        }
        if (codeContenant == null || codeContenant.isBlank()) {
            throw new BusinessException("Le code du contenant est obligatoire.");
        }

        Bac bac = bacRepo.findByCode(codeContenant.trim())
                .or(() -> bacRepo.findByRfidTag(codeContenant.trim()))
                .orElseThrow(() -> new BusinessException("Bac introuvable : " + codeContenant));

        boolean infecte = bac.getTypebac() == TypeBac.INFECTE;

        List<Linge> linges = lingesDeChambre(collecte.getChambreId()).stream()
                .filter(l -> !done(collecteId, l.getId()))
                .collect(Collectors.toList());

        if (linges.isEmpty()) {
            throw new BusinessException("Aucun linge de chambre à traiter pour cette collecte.");
        }

        if (lingeIds != null && !lingeIds.isEmpty()) {
            Set<Long> attendus = linges.stream().map(Linge::getId).collect(Collectors.toSet());
            List<Long> nonAttendus = lingeIds.stream()
                    .filter(id -> !attendus.contains(id)).distinct().collect(Collectors.toList());
            if (!nonAttendus.isEmpty()) {
                throw new BusinessException(
                        "Linges non attendus ou déjà traités pour la chambre : " + nonAttendus);
            }
            Set<Long> coches = new HashSet<>(lingeIds);
            linges = linges.stream().filter(l -> coches.contains(l.getId())).collect(Collectors.toList());
        }

        int nbActuel = lingeRepo.findByBacId(bac.getId()).size();
        int capacite = bac.getCapacite_max() != null ? bac.getCapacite_max().intValue() : Integer.MAX_VALUE;
        int restant = Math.max(0, capacite - nbActuel);
        if (linges.size() > restant) {
            throw new BusinessException("Bac " + bac.getCode() + " insuffisant : " + linges.size()
                    + " linge(s) à mettre, " + restant + " place(s) restante(s). Scannez un autre bac.");
        }

        EtatLinge nouvelEtat = infecte ? EtatLinge.INFECTE_COLLECTE : EtatLinge.COLLECTE;
        StatutCollecteLinge statut = infecte ? StatutCollecteLinge.INFECTE_PRESUME : StatutCollecteLinge.BLANC;
        TypeCollecte typeCollecte = infecte ? TypeCollecte.INFECTE : TypeCollecte.BLANC;
        Long chambreId = collecte.getChambreId();

        List<String> codes = new ArrayList<>();
        for (Linge linge : linges) {
            EtatLinge ancien = linge.getEtat();
            linge.setBac(bac);
            linge.setEtat(nouvelEtat);
            linge.setStatutCollecte(statut);
            linge.setTypeCollecte(typeCollecte);
            linge.setLitId(null);
            linge.setChambreId(null);
            lingeRepo.save(linge);

            collecteLingeRepo.save(CollecteLinge.builder()
                    .collecte(collecte).linge(linge).typeCollecte(typeCollecte)
                    .numeroSacInfecte(infecte ? bac.getCode() : null)
                    .dateScan(LocalDateTime.now()).build());

            historiqueRepo.save(HistoriqueLinge.builder()
                    .linge(linge).etatAvant(ancien).etatApres(nouvelEtat)
                    .personnelId(collecte.getPersonnelId())
                    .typeOperation(infecte ? TypeOperation.DECLARATION_SAC_INFECTE : TypeOperation.COLLECTE)
                    .dateOperation(LocalDateTime.now())
                    .collecte(collecte)
                    .observations((infecte ? "INFECTE" : "BLANC") + " chambre=" + chambreId
                            + " (linge de chambre) contenant=" + bac.getCode())
                    .build());

            codes.add(linge.getCodeBarre());
        }

        return Map.of(
                "chambreId", chambreId,
                "codeContenant", bac.getCode(),
                "typeContenant", infecte ? "BAC_INFECTE" : "BAC_BLANC",
                "estInfecte", infecte,
                "nbTraites", linges.size(),
                "codes", codes);
    }

    @Transactional
    public Map<String, Object> traiterLit(
            Long collecteId,
            Long litId,
            String codeContenant,
            List<Long> lingeIds) {

        Collecte collecte = findC(collecteId);


        if (collecte.getEtatCollecte()
                != EtatCollecte.EN_TRAITEMENT) {

            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }


        if (collecte.getChambreId() == null) {

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

        LitDto lit =
                chambreLitService.getLitById(litId);

        if (lit == null) {

            throw new BusinessException(
                    "Lit introuvable : " + litId
            );
        }

        if (lit.getChambreId() == null
                || !Objects.equals(
                lit.getChambreId(),
                collecte.getChambreId()
        )) {

            throw new BusinessException(
                    "Ce lit n'appartient pas à la chambre "
                            + "de cette collecte."
            );
        }

        boolean infecte =
                chambreLitService
                        .getLitsInfectes()
                        .stream()
                        .anyMatch(l ->
                                Objects.equals(
                                        l.getId(),
                                        litId
                                )
                        );


        Bac bac = null;
        if (infecte) {

            bac =
                    bacRepo.findByCode(codeContenant)
                            .or(() -> bacRepo.findByRfidTag(codeContenant))
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Bac infecté introuvable : "
                                                    + codeContenant
                                    )
                            );

            if (bac.getTypebac() != TypeBac.INFECTE) {
                throw new BusinessException(
                        "Ce lit est infecté. "
                                + "Veuillez scanner un bac INFECTE."
                );
            }

        } else {

            bac =
                    bacRepo.findByCode(codeContenant)
                            .or(() -> bacRepo.findByRfidTag(codeContenant))
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Bac introuvable : " + codeContenant
                                    )
                            );

            if (bac.getTypebac() != TypeBac.BLANC) {
                throw new BusinessException(
                        "Ce lit n'est pas infecté. "
                                + "Veuillez scanner un bac BLANC."
                );
            }
        }


        List<Linge> linges =
                lingeRepo.findByLitIdInAndEtat(
                        List.of(litId),
                        EtatLinge.LIVRE
                );

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


        if (lingeIds != null && !lingeIds.isEmpty()) {

            Set<Long> idsAttendus =
                    linges.stream()
                            .map(Linge::getId)
                            .collect(Collectors.toSet());

            List<Long> nonAttendus =
                    lingeIds.stream()
                            .filter(id -> !idsAttendus.contains(id))
                            .distinct()
                            .collect(Collectors.toList());

            if (!nonAttendus.isEmpty()) {
                throw new BusinessException(
                        "Linges non attendus ou déjà traités pour le lit "
                                + s(lit.getCode())
                                + " : "
                                + nonAttendus
                );
            }

            Set<Long> idsCoches = new HashSet<>(lingeIds);

            linges =
                    linges.stream()
                            .filter(l -> idsCoches.contains(l.getId()))
                            .collect(Collectors.toList());
        }


        if (bac != null) {

            int nbActuel =
                    lingeRepo.findByBacId(bac.getId()).size();

            int capacite =
                    bac.getCapacite_max() != null
                            ? bac.getCapacite_max().intValue()
                            : Integer.MAX_VALUE;

            int restant = Math.max(0, capacite - nbActuel);

            if (linges.size() > restant) {
                throw new BusinessException(
                        "Bac "
                                + bac.getCode()
                                + " insuffisant : "
                                + linges.size()
                                + " linge(s) à mettre, "
                                + restant
                                + " place(s) restante(s). Scannez un autre bac."
                );
            }
        }

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

        String codeContenantFinal = bac.getCode();


        List<String> codes =
                new ArrayList<>();

        for (Linge linge : linges) {

            EtatLinge ancienEtat =
                    linge.getEtat();


            linge.setBac(bac);

            linge.setEtat(
                    nouvelEtat
            );


            linge.setStatutCollecte(
                    statut
            );


            linge.setTypeCollecte(
                    typeCollecte
            );


            linge.setLitId(
                    null
            );


            linge.setChambreId(
                    null
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
                                    typeCollecte
                            )

                            .numeroSacInfecte(
                                    infecte
                                            ? codeContenantFinal
                                            : null
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
        return Map.of(
                "litId",
                litId,

                "codeLit",
                s(lit.getCode()),

                "codeContenant",
                codeContenantFinal,

                "typeContenant",
                infecte ? "BAC_INFECTE" : "BAC_BLANC",

                "estInfecte",
                infecte,

                "nbTraites",
                linges.size(),

                "codes",
                codes
        );
    }

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

        linge.setStatutCollecte(
                StatutCollecteLinge.PROPRE
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
                                TypeCollecte.MIXTE
                        )

                        .dateScan(
                                LocalDateTime.now()
                        )

                        .build()
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

    @Transactional
    public Map<String, Object> annulerCollecteSiVide(
            Long collecteId) {

        Collecte collecte =
                findC(collecteId);


        List<CollecteLinge> items =
                collecteLingeRepo
                        .findByCollecte_Id(
                                collecteId
                        );


        if (!items.isEmpty()) {

            throw new BusinessException(
                    "Cette collecte contient déjà des linges "
                            + "traités. Utilisez la clôture au lieu "
                            + "de l'annulation."
            );
        }


        String codeChambre =
                s(collecte.getCodeChambre());

        collecteRepo.delete(
                collecte
        );


        return Map.of(
                "collecteId",
                collecteId,

                "codeChambre",
                codeChambre,

                "annulee",
                true
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



        if (items.isEmpty()) {

            String codeChambre =
                    s(collecte.getCodeChambre());

            collecteRepo.delete(
                    collecte
            );

            return Map.of(
                    "collecteId",
                    collecteId,

                    "codeChambre",
                    codeChambre,

                    "annulee",
                    true,

                    "message",
                    "Aucun linge collecté : la collecte "
                            + "n'a pas été enregistrée."
            );
        }


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


    private boolean estLingeAttendu(
            Collecte collecte,
            Linge linge) {

        if (collecte == null) {
            return false;
        }


        if (linge == null) {
            return false;
        }


        if (collecte.getChambreId() == null) {
            return false;
        }


        if (linge.getLitId() == null) {
            return false;
        }

        if (linge.getEtat()
                != EtatLinge.LIVRE) {

            return false;
        }



        Long litId =
                linge.getLitId();


        List<Linge> lingesDuLit =
                lingeRepo
                        .findByLitIdInAndEtat(
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

    @Transactional
    public Map<String, Object> declarerLingeInfecte(
            Long collecteId,
            Long lingeId,
            String codeBacInfecte) {


        Collecte collecte = findC(collecteId);

        if (collecte.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }


        if (lingeId == null) {
            throw new BusinessException(
                    "L'identifiant du linge est obligatoire."
            );
        }

        if (codeBacInfecte == null || codeBacInfecte.isBlank()) {
            throw new BusinessException(
                    "Le code du bac infecté est obligatoire."
            );
        }

        Linge linge = lingeRepo.findById(lingeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "id",
                                lingeId
                        )
                );


        if (!estLingeAttendu(collecte, linge)) {
            throw new BusinessException(
                    "Ce linge ne fait pas partie "
                            + "des linges attendus pour cette collecte."
            );
        }


        if (done(collecteId, lingeId)) {
            throw new BusinessException(
                    "Ce linge a déjà été traité pour cette collecte."
            );
        }


        Bac bacInfecte =
                bacRepo.findByCode(codeBacInfecte)
                        .or(() -> bacRepo.findByRfidTag(codeBacInfecte))
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Bac infecté introuvable : "
                                                + codeBacInfecte
                                )
                        );

        if (bacInfecte.getTypebac() != TypeBac.INFECTE) {
            throw new BusinessException(
                    "Le bac " + bacInfecte.getCode() + " n'est pas un bac infecté."
            );
        }


        EtatLinge ancienEtat = linge.getEtat();

        linge.setBac(bacInfecte);

        if (bacInfecte.getEtat() == null
                || bacInfecte.getEtat() == EtatBac.EN_ATTENTE_COLLECTE
                || bacInfecte.getEtat() == EtatBac.VIDE) {
            bacInfecte.setEtat(EtatBac.COLLECTE);
            bacRepo.save(bacInfecte);
        }


        linge.setEtat(
                EtatLinge.INFECTE_COLLECTE
        );

        linge.setStatutCollecte(
                StatutCollecteLinge.INFECTE_PRESUME
        );

        linge.setTypeCollecte(
                TypeCollecte.INFECTE
        );


        linge.setLitId(null);
        linge.setChambreId(null);

        lingeRepo.save(linge);


        collecteLingeRepo.save(
                CollecteLinge.builder()
                        .collecte(collecte)
                        .linge(linge)
                        .typeCollecte(TypeCollecte.INFECTE)
                        .numeroSacInfecte(
                                bacInfecte.getCode()
                        )
                        .dateScan(LocalDateTime.now())
                        .build()
        );

        historiqueRepo.save(
                HistoriqueLinge.builder()
                        .linge(linge)
                        .etatAvant(ancienEtat)
                        .etatApres(
                                EtatLinge.INFECTE_COLLECTE
                        )
                        .personnelId(
                                collecte.getPersonnelId()
                        )
                        .typeOperation(
                                TypeOperation.DECLARATION_SAC_INFECTE
                        )
                        .dateOperation(
                                LocalDateTime.now()
                        )
                        .collecte(collecte)
                        .observations(
                                "Linge déclaré infecté "
                                        + "par l'agent. "
                                        + "Lit="
                                        + linge.getLitId()
                                        + " | Bac="
                                        + bacInfecte.getCode()
                        )
                        .build()
        );



        return Map.of(
                "collecteId",
                collecteId,

                "lingeId",
                linge.getId(),

                "codeBarre",
                linge.getCodeBarre(),

                "codeBacInfecte",
                bacInfecte.getCode(),

                "statut",
                "INFECTE_PRESUME",

                "message",
                "Linge déclaré infecté et placé dans le bac infecté."
        );
    }
}