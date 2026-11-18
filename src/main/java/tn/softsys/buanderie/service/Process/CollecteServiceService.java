package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.service.management.BacInfecteService;

import tn.softsys.buanderie.dto.BacInfecteRequest;

import tn.softsys.buanderie.dto.BacInfecteItemRequest;

import tn.softsys.buanderie.dto.BacInfecteDTO;

import tn.softsys.buanderie.entity.enums.NiveauRattachement;

import tn.softsys.buanderie.service.referentiel.ChambreLitReferentielService;

import tn.softsys.buanderie.dto.ChambreDto;

import tn.softsys.buanderie.service.management.ProfilExploitationService;

import tn.softsys.buanderie.repository.ServiceRepository;
import tn.softsys.buanderie.entity.ServiceEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.*;
import tn.softsys.buanderie.repository.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class CollecteServiceService {

    private final LingeRepository                lingeRepo;
    private final AgentRepository                agentRepo;
    private final ServiceRepository     serviceRepository;
    private final BacRepository                  bacRepo;
    private final CollecteRepository             collecteRepo;
    private final CollecteLingeRepository        collecteLingeRepo;
    private final HistoriqueLingeRepository      historiqueRepo;
    private final BacQuantiteRepository bacQuantiteRepo;
    private final BacInfecteService bacInfecteService;
    private final ProfilExploitationService profilExploitationService;
    private final PersonnelRepository personnelReo;
    private final ObjectMapper objectMapper;
    private final ChambreLitReferentielService chambreLitService;

    @Transactional
    public Map<String, Object> ouvrirService(
            String matriculeAgent,
            Long serviceId,
            String refOuCode) {

        Personnel personnel = personnelReo
                .findByCodeBarre(matriculeAgent);
//                .orElseThrow(() ->
//                        new BusinessException(
//                                "Agent introuvable : " + matriculeAgent
//                        )
//                );

        Optional<ServiceEntity> svc = resolveService(serviceId, refOuCode);

        List<Linge> attendus = lingeRepo.findByServiceIdAndEtat(
                svc.get().getId(),
                EtatLinge.LIVRE
        );

        if (attendus.isEmpty()) {
            throw new BusinessException(
                    "Aucun linge livré à collecter pour ce service"
            );
        }

        /*
         * Snapshot des quantités attendues.
         */
        Map<CategLinge, Integer> attendusParCategorie =
                attendus.stream()
                        .collect(Collectors.groupingBy(
                                Linge::getCategorie,
                                () -> new EnumMap<>(CategLinge.class),
                                Collectors.summingInt(l -> 1)
                        ));

        String quantitesAttendues;

        try {
            quantitesAttendues =
                    objectMapper.writeValueAsString(
                            attendusParCategorie
                    );
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "Impossible de créer le snapshot des quantités attendues"
            );
        }

        /*
         * Nettoyage du statut PROPRE provenant éventuellement
         * d'une ancienne opération.
         */
        attendus.forEach(l -> {
            if (l.getStatutCollecte() == StatutCollecteLinge.PROPRE) {
                l.setStatutCollecte(null);
            }
        });

        lingeRepo.saveAll(attendus);

        Collecte collecte = Collecte.builder()
                .personnelId(personnel.getId())
                .service(svc.get().getId())
                .typeCollecte(TypeCollecte.MIXTE)
                .etatCollecte(EtatCollecte.EN_TRAITEMENT)
                .dateDebut(LocalDateTime.now())
                .nbAttendus(attendus.size())
                .nbScannésBlanc(0)
                .nbPropres(0)
                .nbInfectésPrésumés(0)
                .nbManquants(0)
                .quantitesAttendues(quantitesAttendues)
                .numeroCollecte(genererNumeroCollecte())
                .build();

        collecte = collecteRepo.save(collecte);

        /*
         * Réponse.
         */
        List<Map<String, Object>> lignes =
                attendus.stream()
                        .map(l -> {
                            Map<String, Object> row =
                                    new HashMap<>();

                            row.put("id", l.getId());
                            row.put("codeBarre", l.getCodeBarre());
                            row.put("rfidTag", l.getRfidTag());
                            row.put("categorie", l.getCategorie());
                            row.put("statut", null);

                            return row;
                        })
                        .toList();

        Map<String, Object> response = new HashMap<>();

        response.put("collecteId", collecte.getId());
        response.put("numeroCollecte", collecte.getNumeroCollecte());
        response.put("serviceId", svc.get().getId());
        response.put("nbAttendus", collecte.getNbAttendus());
        response.put("attendusParCategorie", attendusParCategorie);
        response.put("linges", lignes);

        return response;
    }


    @Transactional
    public Map<String, Object> ouvrirChambre(String matriculeAgent, String codeChambre) {
        profilExploitationService.exigerNiveau(NiveauRattachement.CHAMBRE);

        Personnel personnel = matriculeAgent == null || matriculeAgent.isBlank()
                ? null
                : personnelReo.findByCodeBarre(matriculeAgent.trim());
        if (personnel == null) {
            throw new BusinessException("Agent introuvable : " + matriculeAgent);
        }

        ChambreDto chambre = chambreLitService.getChambreByCodeBarre(codeChambre);
        if (chambre == null) {
            throw new BusinessException("Chambre introuvable : " + codeChambre);
        }

        List<Linge> attendus = lingeRepo.findByChambreIdAndEtat(chambre.getId(), EtatLinge.LIVRE);
        if (attendus.isEmpty()) {
            throw new BusinessException("Aucun linge livré à collecter pour cette chambre");
        }

        Map<CategLinge, Integer> attendusParCategorie =
                attendus.stream()
                        .collect(Collectors.groupingBy(
                                Linge::getCategorie,
                                () -> new EnumMap<>(CategLinge.class),
                                Collectors.summingInt(l -> 1)
                        ));

        String quantitesAttendues;
        try {
            quantitesAttendues = objectMapper.writeValueAsString(attendusParCategorie);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Impossible de créer le snapshot des quantités attendues");
        }

        attendus.forEach(l -> {
            if (l.getStatutCollecte() == StatutCollecteLinge.PROPRE) {
                l.setStatutCollecte(null);
            }
        });
        lingeRepo.saveAll(attendus);

        Collecte collecte = Collecte.builder()
                .personnelId(personnel.getId())
                .service(chambre.getServiceId())
                .chambreId(chambre.getId())
                .codeChambre(chambre.getCode())
                .typeCollecte(TypeCollecte.MIXTE)
                .etatCollecte(EtatCollecte.EN_TRAITEMENT)
                .dateDebut(LocalDateTime.now())
                .nbAttendus(attendus.size())
                .nbScannésBlanc(0)
                .nbPropres(0)
                .nbInfectésPrésumés(0)
                .nbManquants(0)
                .quantitesAttendues(quantitesAttendues)
                .numeroCollecte(genererNumeroCollecte())
                .build();
        collecte = collecteRepo.save(collecte);

        List<Map<String, Object>> lignes =
                attendus.stream()
                        .map(l -> {
                            Map<String, Object> row = new HashMap<>();
                            row.put("id", l.getId());
                            row.put("codeBarre", l.getCodeBarre());
                            row.put("rfidTag", l.getRfidTag());
                            row.put("categorie", l.getCategorie());
                            row.put("statut", null);
                            return row;
                        })
                        .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("collecteId", collecte.getId());
        response.put("numeroCollecte", collecte.getNumeroCollecte());
        response.put("chambreId", chambre.getId());
        response.put("codeChambre", chambre.getCode());
        response.put("serviceId", chambre.getServiceId());
        response.put("nbAttendus", collecte.getNbAttendus());
        response.put("attendusParCategorie", attendusParCategorie);
        response.put("linges", lignes);
        return response;
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
        if (c.getChambreId() != null) {
            l.setChambreId(null);
            l.setLitId(null);
        }
//        l.setServiceId(null);
        lingeRepo.save(l);

        collecteLingeRepo.save(CollecteLinge.builder()
                .collecte(c).linge(l)
                .typeCollecte(TypeCollecte.BLANC)
                .dateScan(LocalDateTime.now())

                .build());

        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(l).etatAvant(ancien).etatApres(EtatLinge.COLLECTE)
                .personnelId(c.getPersonnelId())
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
    public BacInfecteDTO declarerQuantitesInfectes(
            Long collecteId,
            BacInfecteRequest request) {
        profilExploitationService.exigerCircuitInfecte();


        /*
         * IMPORTANT :
         * on récupère la collecte avec un verrou pessimiste.
         */
        Collecte c = collecteRepo
                .findByIdForUpdate(collecteId)
                .orElseThrow(() ->
                        new BusinessException(
                                "Collecte introuvable : " + collecteId
                        )
                );

        /*
         * Vérifier l'état de la collecte.
         */
        if (c.getEtatCollecte() != EtatCollecte.EN_TRAITEMENT) {

            throw new BusinessException(
                    "La collecte n'est plus en cours de traitement"
            );
        }

        /*
         * Vérifier le contenu de la requête.
         */
        if (request == null
                || request.getItems() == null
                || request.getItems().isEmpty()) {

            throw new BusinessException(
                    "Le sac infecté doit contenir au moins un article"
            );
        }

        /*
         * Vérification du bac : le bac infecté qui reçoit les quantités.
         */
        if (request.getBacId() == null) {
            throw new BusinessException(
                    "Scannez le bac infecté qui reçoit ces quantités."
            );
        }
        if (request.getBacId() != null) {

            Bac bac = bacRepo.findById(request.getBacId())
                    .orElseThrow(() ->
                            new BusinessException(
                                    "Bac introuvable id="
                                            + request.getBacId()
                            )
                    );

            if (bac.getTypebac() != TypeBac.INFECTE) {

                throw new BusinessException(
                        "Le bac ["
                                + bac.getCode()
                                + "] n'est pas un bac infecté (ROUGE)."
                );
            }
        }


        Map<CategLinge, Long> demandes =
                request.getItems()
                        .stream()
                        .collect(Collectors.groupingBy(
                                BacInfecteItemRequest::getCategorie,
                                () -> new EnumMap<>(CategLinge.class),
                                Collectors.summingLong(
                                        item -> {

                                            if (item.getQuantiteDeclaree()
                                                    == null) {

                                                throw new BusinessException(
                                                        "Quantité obligatoire pour "
                                                                + item.getCategorie()
                                                );
                                            }

                                            if (item.getQuantiteDeclaree()
                                                    <= 0) {

                                                throw new BusinessException(
                                                        "La quantité doit être "
                                                                + "supérieure à zéro pour "
                                                                + item.getCategorie()
                                                );
                                            }

                                            return item.getQuantiteDeclaree();
                                        }
                                )
                        ));

        /*
         * Quantités restantes AVANT cette déclaration.
         */
        Map<CategLinge, Long> restantes =
                getQuantitesRestantes(c);

        /*
         * Validation catégorie par catégorie.
         */
        for (Map.Entry<CategLinge, Long> entry
                : demandes.entrySet()) {

            CategLinge categorie = entry.getKey();

            long demande = entry.getValue();

            long restant =
                    restantes.getOrDefault(categorie, 0L);

            /*
             * Une catégorie qui n'existe pas dans le snapshot
             * ne peut pas être déclarée.
             */
            if (!getAttendusParCategorie(c)
                    .containsKey(categorie)) {

                throw new BusinessException(
                        "La catégorie "
                                + categorie.name()
                                + " ne fait pas partie des linges "
                                + "attendus de cette collecte"
                );
            }

            if (demande > restant) {

                throw new BusinessException(
                        "Quantité infectée déclarée trop élevée pour "
                                + categorie.name()
                                + " : déclaré="
                                + demande
                                + ", restant="
                                + restant
                );
            }
        }

        /*
         * Tout est valide.
         *
         * On délègue maintenant la création du sac.
         */
        return bacInfecteService
                .declarerQuantites(request, c);
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
                .personnelId(c.getPersonnelId())
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
        long nbSacs = bacQuantiteRepo.findByCollecteId(collecteId).stream()
                .map(q -> q.getBac().getId()).distinct().count();

        //  Collecte vide = 0 linge scanné ET 0 sac infecté déclaré
        if (lignes.isEmpty() && nbSacs == 0)
            throw new BusinessException(
                    "Impossible de clôturer : aucune action enregistrée. "
                            + "Scannez au moins un linge ou déclarez des quantités infectées.");

        long b = cnt(lignes, TypeCollecte.BLANC);
        long i = cnt(lignes, TypeCollecte.INFECTE);
        long p = cnt(lignes, TypeCollecte.PROPRE_EN_PLACE);
        long m = cnt(lignes, TypeCollecte.MANQUANT);

        // Total infectés = lignes INFECTE + quantités dans sacs infectés
        long qteInfectes = bacQuantiteRepo.findByCollecteId(collecteId).stream()
                .mapToLong(q -> q.getQuantiteDeclaree() != null ? q.getQuantiteDeclaree() : 0L)
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
                "nbBacsInfectes", nbSacs,
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

    private Map<CategLinge, Integer> getAttendusParCategorie(
            Collecte collecte) {

        if (collecte.getQuantitesAttendues() == null
                || collecte.getQuantitesAttendues().isBlank()) {

            throw new BusinessException(
                    "Le snapshot des quantités attendues est absent"
            );
        }

        try {

            return objectMapper.readValue(
                    collecte.getQuantitesAttendues(),
                    new TypeReference<EnumMap<CategLinge, Integer>>() {}
            );

        } catch (JsonProcessingException e) {

            throw new BusinessException(
                    "Snapshot des quantités attendu invalide"
            );
        }
    }

    private Map<CategLinge, Long> getQuantitesTraitees(
            Long collecteId) {

        Map<CategLinge, Long> result =
                new EnumMap<>(CategLinge.class);

        /*
         * 1. Linges individuels :
         * BLANC
         * PROPRE_EN_PLACE
         * MANQUANT
         */
        List<CollecteLinge> lignes =
                collecteLingeRepo.findByCollecte_Id(collecteId);

        for (CollecteLinge cl : lignes) {

            if (cl.getLinge() == null) {
                continue;
            }

            TypeCollecte type = cl.getTypeCollecte();

            if (type != TypeCollecte.BLANC
                    && type != TypeCollecte.PROPRE_EN_PLACE
                    && type != TypeCollecte.MANQUANT) {

                continue;
            }

            CategLinge categorie =
                    cl.getLinge().getCategorie();

            result.merge(
                    categorie,
                    1L,
                    Long::sum
            );
        }


        for (BacQuantite q : bacQuantiteRepo.findByCollecteId(collecteId)) {

            if (q.getCategorie() == null || q.getQuantiteDeclaree() == null) {
                continue;
            }

            result.merge(
                    q.getCategorie(),
                    (long) q.getQuantiteDeclaree(),
                    Long::sum
            );
        }

        return result;
    }
    private Map<CategLinge, Long> getQuantitesRestantes(
            Collecte collecte) {

        Map<CategLinge, Integer> attendus =
                getAttendusParCategorie(collecte);

        Map<CategLinge, Long> traites =
                getQuantitesTraitees(collecte.getId());

        Map<CategLinge, Long> restantes =
                new EnumMap<>(CategLinge.class);

        for (Map.Entry<CategLinge, Integer> entry
                : attendus.entrySet()) {

            CategLinge categorie = entry.getKey();

            long attendu = entry.getValue();

            long traite =
                    traites.getOrDefault(categorie, 0L);

            long restant = attendu - traite;

            if (restant < 0) {

                throw new BusinessException(
                        "Incohérence de quantité pour "
                                + categorie.name()
                                + " : attendu=" + attendu
                                + ", traité=" + traite
                );
            }

            restantes.put(
                    categorie,
                    restant
            );
        }

        return restantes;
    }
    private String genererNumeroCollecte() {

        return "COL-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + "-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();
    }
    private Optional<ServiceEntity> resolveService(Long serviceId, String refOuCode) {

        if (serviceId != null) {
            return serviceRepository.findById(serviceId);
//                    .orElseThrow(() ->
//                            new BusinessException(
//                                    "Service introuvable avec l'id : " + serviceId
//                            )
//                    );
        }

        if (refOuCode == null || refOuCode.isBlank()) {
            throw new BusinessException(
                    "Le service doit être identifié par son id, sa référence ou son code"
            );
        }

        String valeur = refOuCode.trim();

        return serviceRepository
                .findByReferenceIgnoreCaseOrCodeIgnoreCase(valeur, valeur);
//                .orElseThrow(() ->
//                        new BusinessException(
//                                "Service introuvable : " + valeur
//                        )
//                );
    }
}