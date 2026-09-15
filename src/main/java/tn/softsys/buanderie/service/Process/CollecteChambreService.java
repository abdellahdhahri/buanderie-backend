package tn.softsys.buanderie.service.Process;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.*;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.*;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Chambre;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Lit;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.*;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.management.SacInfecteService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service @RequiredArgsConstructor @Transactional
public class CollecteChambreService {

    private final LingeRepository lingeRepo;
    private final AgentRepository agentRepo;
    private final BacRepository bacRepo;
    private final CollecteRepository collecteRepo;
    private final CollecteLingeRepository collecteLingeRepo;
    private final HistoriqueLingeRepository historiqueRepo;
    private final SqlServerChambreRepository sqlChambreRepo;
    private final SqlServerLitRepository sqlLitRepo;
    private final SqlServerSejourRepository sqlSejourRepo;
    private final SqlServerPersonnelRepository personnelReo;
    private final SacInfecteRepository sacInfecteRepository;
    private final SacInfecteService sacInfecteService;


    @Transactional(readOnly = true)
    public Map<String, Object> scannerLit(
            Long collecteId,
            Long litId) {

        Collecte c = findC(collecteId);

        if (c.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }

        if (c.getChambreIdSqlServer() == null) {
            throw new BusinessException(
                    "La chambre de la collecte est introuvable."
            );
        }

        Lit lit = sqlLitRepo.findById(litId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lit",
                                "id",
                                litId
                        )
                );

        // Vérifier que le lit appartient à la chambre
        if (lit.getChambre() == null
                || !Objects.equals(
                lit.getChambre().getId(),
                c.getChambreIdSqlServer()
        )) {

            throw new BusinessException(
                    "Ce lit n'appartient pas à la chambre de cette collecte."
            );
        }

        boolean infecte =
                sqlSejourRepo.findLitsInfectes()
                        .stream()
                        .anyMatch(l ->
                                Objects.equals(l.getId(), litId)
                        );

        List<Linge> linges =
                lingeRepo.findByLitIdSqlServerInAndEtat(
                        List.of(litId),
                        EtatLinge.LIVRE
                );

        if (linges.isEmpty()) {
            throw new BusinessException(
                    "Aucun linge LIVRE à traiter pour ce lit."
            );
        }


        List<Linge> lingesNonTraites =
                linges.stream()
                        .filter(l ->
                                !done(collecteId, l.getId())
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

        Personnel personnel = find(matriculeAgent);

        Long personnelId = personnel.getId();

        Chambre chambre =
                sqlChambreRepo
                        .findByCodeBarre(codeBarre)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Chambre introuvable : "
                                                + codeBarre
                                )
                        );



        Set<Long> infectes =
                sqlSejourRepo.findLitsInfectes()
                        .stream()
                        .map(Lit::getId)
                        .collect(Collectors.toSet());



        List<Lit> lits =
                sqlLitRepo.findByChambreId(
                        chambre.getId(),
                        PageRequest.of(0, 50)
                ).getContent();



        List<Map<String, Object>> litsDto =
                lits.stream()
                        .map(lit -> {

                            // IMPORTANT :
                            // On récupère tous les LIVRE.
                            // On ne filtre plus sur statutCollecte == null.
                            List<Linge> lg =
                                    lingeRepo
                                            .findByLitIdSqlServerInAndEtat(
                                                    List.of(lit.getId()),
                                                    EtatLinge.LIVRE
                                            )
                                            .stream()
                                            // Éviter les linges déjà traités
                                            // dans cette collecte : ici il n'y
                                            // a pas encore de collecte créée,
                                            // donc on garde tous les LIVRE.
                                            .collect(Collectors.toList());

                            return Map.<String, Object>of(
                                    "litId",
                                    lit.getId(),

                                    "codeLit",
                                    s(lit.getCode()),

                                    "estInfecte",
                                    infectes.contains(lit.getId()),

                                    "nbLinges",
                                    lg.size(),

                                    "linges",
                                    lg.stream()
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
                                                                    : ""
                                                    )
                                            )
                                            .collect(Collectors.toList())
                            );
                        })
                        .filter(m ->
                                (int) m.get("nbLinges") > 0
                        )
                        .collect(Collectors.toList());



        if (litsDto.isEmpty()) {
            throw new BusinessException(
                    "Aucun linge LIVRE — chambre "
                            + chambre.getCode()
            );
        }

        int nb =
                litsDto.stream()
                        .mapToInt(m ->
                                (int) m.get("nbLinges")
                        )
                        .sum();



        Collecte c =
                collecteRepo.save(
                        Collecte.builder()
                                .personnelId(personnelId)
                                .chambreIdSqlServer(chambre.getId())
                                .codeChambre(chambre.getCode())
                                .typeCollecte(TypeCollecte.MIXTE)
                                .etatCollecte(EtatCollecte.EN_TRAITEMENT)
                                .service(chambre.getService().getId())
                                .nbAttendus(nb)
                                .numeroCollecte(
                                        "CH-" + System.currentTimeMillis()
                                )
                                .build()
                );

        return Map.of(
                "collecteId",
                c.getId(),

                "personnelId",
                personnelId,

                "codeChambre",
                chambre.getCode(),

                "nbLits",
                litsDto.size(),

                "nbLinges",
                nb,

                "lits",
                litsDto
        );
    }


    @Transactional
    public Map<String, Object> traiterLit(
            Long collecteId,
            Long litId,
            String codeContenant) {

        Collecte c = findC(collecteId);

        if (c.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }



        if (c.getChambreIdSqlServer() == null) {
            throw new BusinessException(
                    "La chambre de la collecte est introuvable."
            );
        }

        Lit lit = sqlLitRepo.findById(litId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lit",
                                "id",
                                litId
                        )
                );

        if (lit.getChambre() == null
                || !Objects.equals(
                lit.getChambre().getId(),
                c.getChambreIdSqlServer()
        )) {

            throw new BusinessException(
                    "Ce lit n'appartient pas à la chambre de cette collecte."
            );
        }



        boolean infecte =
                sqlSejourRepo.findLitsInfectes()
                        .stream()
                        .anyMatch(l ->
                                Objects.equals(l.getId(), litId)
                        );


        Bac bac = null;
        SacInfecte sacInfecte = null;

        if (infecte) {

            sacInfecte =
                    sacInfecteRepository
                            .findByNumeroSac(codeContenant)
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Sac infecté introuvable : "
                                                    + codeContenant
                                    )
                            );

        } else {

            bac =
                    bacRepo.findByCode(codeContenant)
                            .or(() ->
                                    bacRepo.findByRfidTag(codeContenant)
                            )
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Bac introuvable : "
                                                    + codeContenant
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
                lingeRepo.findByLitIdSqlServerInAndEtat(
                        List.of(litId),
                        EtatLinge.LIVRE
                );


        linges =
                linges.stream()
                        .filter(l ->
                                !done(collecteId, l.getId())
                        )
                        .collect(Collectors.toList());

        if (linges.isEmpty()) {
            throw new BusinessException(
                    "Aucun linge LIVRE à traiter pour ce lit."
            );
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

        List<String> codes = new ArrayList<>();

        String codeContenantFinal =
                infecte
                        ? sacInfecte.getNumeroSac()
                        : bac.getCode();



        for (Linge l : linges) {

            EtatLinge ancienEtat = l.getEtat();

            if (infecte) {

                l.setSacInfecte(sacInfecte);

                sacInfecteService.ajouterLingeAuSac(
                        sacInfecte,
                        l
                );

            } else {

                l.setBac(bac);
            }

            l.setEtat(nouvelEtat);
            l.setStatutCollecte(statut);
            l.setChambreIdSqlServer(null);
            l.setTypeCollecte(typeCollecte);
            l.setLitIdSqlServer(null);

            lingeRepo.save(l);



            collecteLingeRepo.save(
                    CollecteLinge.builder()
                            .collecte(c)
                            .linge(l)
                            .typeCollecte(typeCollecte)
                            .numeroSacInfecte(
                                    infecte
                                            ? sacInfecte.getNumeroSac()
                                            : null
                            )
                            .dateScan(LocalDateTime.now())
                            .build()
            );



            historiqueRepo.save(
                    HistoriqueLinge.builder()
                            .linge(l)
                            .etatAvant(ancienEtat)
                            .etatApres(nouvelEtat)
                            .personnelId(c.getPersonnelId())
                            .typeOperation(
                                    infecte
                                            ? TypeOperation
                                              .DECLARATION_SAC_INFECTE
                                            : TypeOperation.COLLECTE
                            )
                            .dateOperation(LocalDateTime.now())
                            .collecte(c)
                            .observations(
                                    (infecte
                                            ? "INFECTE"
                                            : "BLANC")
                                            + " lit=" + litId
                                            + " contenant="
                                            + codeContenantFinal
                            )
                            .build()
            );

            codes.add(l.getCodeBarre());
        }



        return Map.of(
                "litId",
                litId,

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


    @Transactional
    public Map<String, Object> declarerPropre(
            Long collecteId,
            String codeOuTag) {

        Collecte c = findC(collecteId);

        if (c.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }

        Linge l = findL(codeOuTag);

        // Vérifier que le linge appartient à la collecte
        if (!estLingeAttendu(c, l)) {
            throw new BusinessException(
                    "Ce linge ne fait pas partie des linges attendus "
                            + "pour cette collecte."
            );
        }

        // Déjà traité
        if (done(collecteId, l.getId())) {
            throw new BusinessException(
                    "Ce linge a déjà été traité pour cette collecte."
            );
        }

        // Le linge reste LIVRE.
        // Seul le statut de collecte devient PROPRE.
        l.setStatutCollecte(
                StatutCollecteLinge.PROPRE
        );

        lingeRepo.save(l);

        return Map.of(
                "lingeId",
                l.getId(),

                "codeBarre",
                l.getCodeBarre(),

                "statut",
                "PROPRE"
        );
    }


    @Transactional
    public Map<String, Object> declarerManquant(
            Long collecteId,
            Long lingeId) {

        Collecte c = findC(collecteId);

        if (c.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }

        Linge l =
                lingeRepo.findById(lingeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Linge",
                                        "id",
                                        lingeId
                                )
                        );

        // Vérifier que le linge appartient à la collecte
        if (!estLingeAttendu(c, l)) {
            throw new BusinessException(
                    "Ce linge ne fait pas partie des linges attendus "
                            + "pour cette collecte."
            );
        }

        if (done(collecteId, lingeId)) {
            throw new BusinessException(
                    "Ce linge a déjà été traité."
            );
        }

        EtatLinge ancienEtat = l.getEtat();

        l.setStatutCollecte(
                StatutCollecteLinge.MANQUANT
        );

        lingeRepo.save(l);

        collecteLingeRepo.save(
                CollecteLinge.builder()
                        .collecte(c)
                        .linge(l)
                        .typeCollecte(TypeCollecte.MANQUANT)
                        .dateScan(LocalDateTime.now())
                        .build()
        );

        historiqueRepo.save(
                HistoriqueLinge.builder()
                        .linge(l)
                        .etatAvant(ancienEtat)
                        .etatApres(EtatLinge.MANQUANT)
                        .personnelId(c.getPersonnelId())
                        .typeOperation(
                                TypeOperation.CHANGEMENT_ETAT_LINGE
                        )
                        .dateOperation(LocalDateTime.now())
                        .collecte(c)
                        .observations(
                                "Manquant ch="
                                        + c.getCodeChambre()
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
    public Map<String, Object> cloturer(Long collecteId) {

        Collecte c = findC(collecteId);

        if (c.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {
            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement."
            );
        }

        List<CollecteLinge> it =
                collecteLingeRepo.findByCollecte_Id(collecteId);

        long blancs =
                cnt(it, TypeCollecte.BLANC);

        long infectes =
                cnt(it, TypeCollecte.INFECTE);

        long manquants =
                cnt(it, TypeCollecte.MANQUANT);

        long attendus = c.getNbAttendus();

        // Les propres ne sont pas enregistrés dans CollecteLinge.
        long propres =
                attendus
                        - blancs
                        - infectes
                        - manquants;

        if (propres < 0) {
            throw new BusinessException(
                    "Incohérence des quantités : "
                            + "BLANC + INFECTE + MANQUANT > ATTENDU."
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

        c.setNbScannésBlanc((int) blancs);
        c.setNbInfectésPrésumés((int) infectes);
        c.setNbPropres((int) propres);
        c.setNbManquants((int) manquants);

        c.setDateFin(LocalDateTime.now());

        // IMPORTANT :
        // Remplacer TERMINEE par le nom exact de ton enum
        // si ton enum utilise un autre nom.
        c.setEtatCollecte(EtatCollecte.EN_TRAITEMENT);

        collecteRepo.save(c);

        return Map.of(
                "collecteId",
                collecteId,

                "codeChambre",
                s(c.getCodeChambre()),

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


    private Personnel find(String m) {
        return personnelReo.findByCodeBarre(m);
    }

    private Collecte findC(Long id) {
        return collecteRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collecte",
                                "id",
                                id
                        )
                );
    }

    private Linge findL(String c) {
        return lingeRepo
                .findByCodeBarre(c)
                .or(() ->
                        lingeRepo.findByBarcodeOrRfid(c)
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "code",
                                c
                        )
                );
    }

    private boolean done(Long collecteId, Long lingeId) {
        return collecteLingeRepo
                .existsByCollecte_IdAndLinge_Id(
                        collecteId,
                        lingeId
                );
    }

    private long cnt(
            List<CollecteLinge> it,
            TypeCollecte type) {

        return it.stream()
                .filter(i ->
                        type == i.getTypeCollecte()
                )
                .count();
    }

    private String s(String v) {
        return v != null ? v : "";
    }


    private boolean estLingeAttendu(
            Collecte collecte,
            Linge linge) {

        if (collecte.getChambreIdSqlServer() == null) {
            return false;
        }

        if (linge.getLitIdSqlServer() == null) {
            return false;
        }

        // Le linge doit être LIVRE pour faire partie
        // des linges attendus.
        if (linge.getEtat() != EtatLinge.LIVRE) {
            return false;
        }

        List<Linge> attendus =
                lingeRepo.findByLitIdSqlServerInAndEtat(
                        List.of(linge.getLitIdSqlServer()),
                        EtatLinge.LIVRE
                );

        return attendus.stream()
                .anyMatch(l ->
                        Objects.equals(
                                l.getId(),
                                linge.getId()
                        )
                );
    }
}


