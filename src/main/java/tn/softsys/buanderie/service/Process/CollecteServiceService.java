package tn.softsys.buanderie.service.Process;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerPersonnelRepository;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerServiceRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;
import tn.softsys.buanderie.dto.SacsInfectes.*;
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
public class CollecteServiceService {

    private final LingeRepository                lingeRepo;
    private final AgentRepository                agentRepo;
    private final SqlServerServiceRepository     sqlServiceRepo;
    private final BacRepository                  bacRepo;
    private final CollecteRepository             collecteRepo;
    private final CollecteLingeRepository        collecteLingeRepo;
    private final HistoriqueLingeRepository      historiqueRepo;
    private final SacInfecteRepository           sacInfecteRepo;
    private final SacInfecteService              sacInfecteService;
    private final SqlServerPersonnelRepository personnelReo;


    public Map<String, Object> ouvrirService(
            String matriculeAgent,
            Long serviceId,
            String refOuCode) {

        // ============================================
        // 1. Récupérer le Personnel depuis SQL Server
        // ============================================

        Personnel personnel = personnelReo.findByCodeBarre(matriculeAgent);

        if (personnel == null) {
            throw new ResourceNotFoundException(
                    "Personnel",
                    "codeBarre",
                    matriculeAgent
            );
        }

        Long personnelId = personnel.getId();




        tn.softsys.buanderie.Sqlserver.sqlServerEntities.Service svc = null;

        if (serviceId != null) {
            svc = sqlServiceRepo.findById(serviceId).orElse(null);
        }

        if (svc == null && refOuCode != null && !refOuCode.isBlank()) {

            svc = sqlServiceRepo.findByReference(refOuCode);

            if (svc == null) {
                svc = sqlServiceRepo.findByCode(refOuCode);
            }
        }

        if (svc == null) {
            throw new BusinessException(
                    "Service introuvable (id=" + serviceId
                            + ", ref=" + refOuCode + ")"
            );
        }




        List<Linge> attendus =
                lingeRepo.findByServiceIdAndEtat(
                        svc.getId(),
                        EtatLinge.LIVRE
                );



        attendus.forEach(l -> {

            if (l.getStatutCollecte()
                    == StatutCollecteLinge.PROPRE) {

                l.setStatutCollecte(null);
                lingeRepo.save(l);
            }
        });



        if (attendus.isEmpty()) {
            throw new BusinessException(
                    "Aucun linge LIVRE pour le service ["
                            + svc.getCode()
                            + "]. Impossible d'ouvrir une collecte."
            );
        }




        Collecte collecte =
                collecteRepo.save(
                        Collecte.builder()

                                // ID du Personnel SQL Server
                                .personnelId(personnelId)

                                // ID du service SQL Server
                                .service(svc.getId())

                                .typeCollecte(
                                        TypeCollecte.MIXTE
                                )

                                .etatCollecte(
                                        EtatCollecte.EN_TRAITEMENT
                                )

                                .nbAttendus(
                                        attendus.size()
                                )

                                .numeroCollecte(
                                        "SVC-"
                                                + System.currentTimeMillis()
                                )

                                .build()
                );




        List<Map<String, Object>> lingesDto =
                attendus.stream()
                        .map(l ->
                                Map.<String, Object>of(
                                        "lingeId",
                                        l.getId(),

                                        "codeBarre",
                                        l.getCodeBarre(),

                                        "rfid",
                                        l.getRfidTag() != null
                                                ? l.getRfidTag()
                                                : "",

                                        "categorie",
                                        l.getCategorie().name(),

                                        "statut",
                                        ""
                                )
                        )
                        .collect(Collectors.toList());


        return Map.of(
                "collecteId",
                collecte.getId(),

                "personnelId",
                personnelId,

                "serviceId",
                svc.getId(),

                "serviceCode",
                svc.getCode(),

                "serviceRef",
                svc.getReference() != null
                        ? svc.getReference()
                        : "",

                "nbAttendus",
                attendus.size(),

                "linges",
                lingesDto
        );
    }


    //  SCAN BAC — identifie le bac (blanc ou rouge) + vérifie capacité
    public Map<String, Object> scanBac(Long collecteId, String codeOuTag) {
        findC(collecteId); // vérifier que la collecte existe

        Bac bac = bacRepo.findByCode(codeOuTag)
                .or(() -> bacRepo.findByRfidTag(codeOuTag))
                .orElseThrow(() -> new BusinessException(
                        "Bac introuvable : " + codeOuTag));

        int nbActuel = lingeRepo.findByBacId(bac.getId()).size();
        int capacite  = bac.getCapacite_max() != null
                ? bac.getCapacite_max().intValue() : 999;
        int restant   = Math.max(0, capacite - nbActuel);

        return Map.of(
                "bacId",     bac.getId(),
                "codeBac",   bac.getCode(),
                "typeBac",   bac.getTypebac() != null ? bac.getTypebac().name() : "BLANC",
                "capacite",  capacite,
                "nbActuel",  nbActuel,
                "restant",   restant,
                "plein",     restant == 0
        );
    }

    //  SCAN LINGE BLANC — bacId obligatoire (récupéré via scanBac)

    @Transactional
    public Map<String, Object> scanLingeBlanc(Long collecteId,
                                              Long bacId,
                                              String codeOuTag) {
        Collecte c   = findC(collecteId);
        Linge    l   = findL(codeOuTag);
        Bac      bac = bacRepo.findById(bacId)
                .orElseThrow(() -> new ResourceNotFoundException("Bac","id",bacId));

        if (done(collecteId, l.getId()))
            throw new BusinessException("Linge déjà traité dans cette collecte.");

        // Vérifier capacité bac
        int nbActuel = lingeRepo.findByBacId(bacId).size();
        if (bac.getCapacite_max() != null && nbActuel >= bac.getCapacite_max())
            throw new BusinessException(
                    "Bac [" + bac.getCode() + "] plein ("
                            + bac.getCapacite_max().intValue() + " max).");

        EtatLinge ancien = l.getEtat();
        l.setBac(bac);
        l.setEtat(EtatLinge.COLLECTE);
        l.setStatutCollecte(StatutCollecteLinge.BLANC);
        l.setTypeCollecte(TypeCollecte.BLANC);
//        l.setServiceId(null);
        lingeRepo.save(l);

        collecteLingeRepo.save(CollecteLinge.builder()
                .collecte(c).linge(l)
                .typeCollecte(TypeCollecte.BLANC)
                .dateScan(LocalDateTime.now())

                .build());

        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(l).etatAvant(ancien).etatApres(EtatLinge.COLLECTE)
               // .agent(c.getAgent())
                .typeOperation(TypeOperation.COLLECTE)
                .dateOperation(LocalDateTime.now()).collecte(c)
                .observations("BLANC bac=" + bac.getCode()
                        + " svcId=" + c.getService())
                .build());

        return Map.of(
                "lingeId",   l.getId(),
                "codeBarre", l.getCodeBarre(),
                "categorie", l.getCategorie().name(),
                "statut",    "BLANC",
                "bac",       bac.getCode()
        );
    }




    @Transactional
    public SacInfecteDTO declarerSacInfecte(Long collecteId,
                                            SacInfecteRequest request) {
        Collecte c = findC(collecteId);

        // Vérifier que le bac rouge existe
        if (request.getBacId() != null) {
            Bac bacRouge = bacRepo.findById(request.getBacId())
                    .orElseThrow(() -> new BusinessException(
                            "Bac rouge introuvable id=" + request.getBacId()));
            if (bacRouge.getTypebac() != TypeBac.INFECTE)
                throw new BusinessException(
                        "Le bac [" + bacRouge.getCode() + "] n'est pas un bac infecté (ROUGE).");
        }

        return sacInfecteService.declarerSacInfecte(request, c);
    }

    //  PROPRE — linge laissé en place (ne part pas en lavage)
    @Transactional
    public Map<String, Object> declarerPropre(Long collecteId, String codeOuTag) {
        Collecte c = findC(collecteId);
        Linge    l = findL(codeOuTag);
        if (done(collecteId, l.getId()))
            throw new BusinessException("Déjà traité.");
        l.setStatutCollecte(StatutCollecteLinge.PROPRE);
        lingeRepo.save(l);
        collecteLingeRepo.save(CollecteLinge.builder()
                .collecte(c).linge(l)
                .typeCollecte(TypeCollecte.PROPRE_EN_PLACE)
                .dateScan(LocalDateTime.now())
                .build());
        return Map.of("lingeId", l.getId(),
                "codeBarre", l.getCodeBarre(), "statut", "PROPRE");
    }

    //  MANQUANT
    @Transactional
    public Map<String, Object> declarerManquant(Long collecteId, Long lingeId) {
        Collecte c = findC(collecteId);
        Linge    l = lingeRepo.findById(lingeId)
                .orElseThrow(() -> new ResourceNotFoundException("Linge","id",lingeId));
        EtatLinge ancien = l.getEtat();
        l.setStatutCollecte(StatutCollecteLinge.MANQUANT);
        lingeRepo.save(l);
        collecteLingeRepo.save(CollecteLinge.builder()
                .collecte(c).linge(l)
                .typeCollecte(TypeCollecte.MANQUANT)
                .dateScan(LocalDateTime.now())
                .build());
        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(l).etatAvant(ancien).etatApres(EtatLinge.MANQUANT)
                //.agent(c.getAgent())
                .typeOperation(TypeOperation.CHANGEMENT_ETAT_LINGE)
                .dateOperation(LocalDateTime.now()).collecte(c)
                .observations("Manquant svcId=" + c.getService())
                .build());
        return Map.of("lingeId", lingeId, "statut", "MANQUANT");
    }

    // CLÔTURER
    @Transactional
    public Map<String, Object> cloturer(Long collecteId) {
        Collecte c = findC(collecteId);

        List<CollecteLinge> lignes = collecteLingeRepo.findByCollecte_Id(collecteId);

        // Compter aussi les sacs infectés déclarés sur cette collecte
        long nbSacs = sacInfecteRepo.findByCollecte_Id(collecteId).size();

        //  Collecte vide = 0 linge scanné ET 0 sac infecté déclaré
        if (lignes.isEmpty() && nbSacs == 0)
            throw new BusinessException(
                    "Impossible de clôturer : aucune action enregistrée. "
                            + "Scannez au moins un linge ou déclarez un sac infecté.");

        long b = cnt(lignes, TypeCollecte.BLANC);
        long i = cnt(lignes, TypeCollecte.INFECTE);
        long p = cnt(lignes, TypeCollecte.PROPRE_EN_PLACE);
        long m = cnt(lignes, TypeCollecte.MANQUANT);

        // Total infectés = lignes INFECTE + quantités dans sacs infectés
        long qteInfectes = sacInfecteRepo.findByCollecte_Id(collecteId).stream()
                .mapToLong(sac -> sac.getItems() != null
                        ? sac.getItems().stream().mapToLong(si -> si.getQuantiteDeclaree()).sum()
                        : 0L)
                .sum();

        c.setNbScannésBlanc((int) b);
        c.setNbPropres((int) p);
        c.setNbInfectésPrésumés((int) (i + qteInfectes));
        c.setNbManquants((int) m);
        c.setEtatCollecte(EtatCollecte.EN_TRAITEMENT);
        c.setDateFin(LocalDateTime.now());
        collecteRepo.save(c);

        return Map.of(
                "collecteId",   collecteId,
                "serviceId",    c.getService() != null ? c.getService() : 0L,
                "nbBlancs",     b,
                "nbInfectes",   i + qteInfectes,
                "nbSacsInfectes", nbSacs,
                "nbPropres",    p,
                "nbManquants",  m
        );
    }

    // ── Utilitaires privés ────────────────────────────────────────────
    private Collecte findC(Long id) {
        return collecteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecte","id",id));
    }
    private Linge findL(String cd) {
        return lingeRepo.findByCodeBarre(cd)
                .or(() -> lingeRepo.findByBarcodeOrRfid(cd))
                .orElseThrow(() -> new ResourceNotFoundException("Linge","code",cd));
    }
    private boolean done(Long c, Long l) {
        return collecteLingeRepo.existsByCollecte_IdAndLinge_Id(c, l);
    }
    private long cnt(List<CollecteLinge> it, TypeCollecte t) {
        return it.stream().filter(x -> t == x.getTypeCollecte()).count();
    }
}
