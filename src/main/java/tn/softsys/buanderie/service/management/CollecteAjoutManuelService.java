package tn.softsys.buanderie.service.Process;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.AjoutLingesCollecteRequest;
import tn.softsys.buanderie.entity.Bac;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.CollecteLinge;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.SacInfecte;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.EtatCollecte;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.EtatSac;
import tn.softsys.buanderie.entity.enums.StatutCollecteLinge;
import tn.softsys.buanderie.entity.enums.TypeBac;
import tn.softsys.buanderie.entity.enums.TypeCollecte;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.BacRepository;
import tn.softsys.buanderie.repository.CollecteLingeRepository;
import tn.softsys.buanderie.repository.CollecteRepository;
import tn.softsys.buanderie.repository.HistoriqueLingeRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.repository.RemiseLingeCollecteRepository;
import tn.softsys.buanderie.repository.SacInfecteRepository;
import tn.softsys.buanderie.service.management.SacInfecteService;


@Service
@RequiredArgsConstructor
public class CollecteAjoutManuelService {

    private enum Nature { CHAMBRE, SERVICE, UNIFORME }

    private static final Set<EtatLinge> ETATS_LINGE_AUTORISES = EnumSet.of(
            EtatLinge.LIVRE,
            EtatLinge.STOCKE,
            EtatLinge.MANQUANT,
            EtatLinge.PROPRE,
            EtatLinge.SALE
    );

    private static final Set<EtatBac> ETATS_BAC_AUTORISES = EnumSet.of(
            EtatBac.EN_ATTENTE_COLLECTE,
            EtatBac.COLLECTE,
            EtatBac.VIDE
    );

    private final CollecteRepository collecteRepo;
    private final CollecteLingeRepository collecteLingeRepo;
    private final LingeRepository lingeRepo;
    private final BacRepository bacRepo;
    private final SacInfecteRepository sacInfecteRepo;
    private final SacInfecteService sacInfecteService;
    private final HistoriqueLingeRepository historiqueRepo;
    private final RemiseLingeCollecteRepository remiseCollecteRepo;
    private final PersonnelRepository personnelRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> ajouterLinges(Long collecteId,
                                             AjoutLingesCollecteRequest request) {

        if (request == null) {
            throw new BusinessException("La requête est obligatoire.");
        }

        List<String> codes = request.getCodesLinges() == null
                ? new ArrayList<>()
                : request.getCodesLinges().stream()
                  .filter(c -> c != null && !c.isBlank())
                  .map(String::trim)
                  .distinct()
                  .collect(Collectors.toList());

        if (codes.isEmpty()) {
            throw new BusinessException("Veuillez fournir au moins un linge à ajouter.");
        }

        if (request.getCodeContenant() == null || request.getCodeContenant().isBlank()) {
            throw new BusinessException(
                    "Le code du contenant (bac ou sac infecté) est obligatoire.");
        }

        // ---- Collecte ----
        Collecte collecte = collecteRepo.findByIdForUpdate(collecteId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecte", "id", collecteId));

        if (collecte.getEtatCollecte() == EtatCollecte.CLOTUREE
                || collecte.getEtatCollecte() == EtatCollecte.PARTIELLEMENT_LIVREE) {
            throw new BusinessException(
                    "La collecte est déjà clôturée (état : " + collecte.getEtatCollecte() + ").");
        }

        if (!remiseCollecteRepo.findRemisesDeCollecte(collecteId).isEmpty()) {
            throw new BusinessException(
                    "Cette collecte est déjà incluse dans une remise : modification impossible.");
        }

        Nature nature = natureDe(collecte);

        // ---- Traçabilité ----
        Long personnelId = collecte.getPersonnelId();
        if (request.getMatriculeAdmin() != null && !request.getMatriculeAdmin().isBlank()) {
            Personnel admin = personnelRepo.findByCodeBarre(request.getMatriculeAdmin().trim());
            if (admin == null) {
                throw new ResourceNotFoundException(
                        "Agent", "matricule", request.getMatriculeAdmin());
            }
            personnelId = admin.getId();
        }

        // ---- Contenant ----
        String codeContenant = request.getCodeContenant().trim();
        SacInfecte sac = sacInfecteRepo.findByNumeroSac(codeContenant).orElse(null);
        Bac bac = null;

        if (sac != null) {
            if (nature == Nature.UNIFORME) {
                throw new BusinessException(
                        "Une collecte d'uniformes utilise des bacs : scannez un bac.");
            }
            if (sac.getCollecte() == null
                    || !Objects.equals(sac.getCollecte().getId(), collecteId)) {
                throw new BusinessException(
                        "Le sac " + sac.getNumeroSac() + " n'appartient pas à cette collecte.");
            }
            if (sac.getEtat() != EtatSac.DECLARE) {
                throw new BusinessException(
                        "Le sac " + sac.getNumeroSac() + " n'est plus modifiable (état : "
                                + sac.getEtat() + ").");
            }
        } else {
            bac = bacRepo.findByCode(codeContenant)
                    .or(() -> bacRepo.findByRfidTag(codeContenant))
                    .orElseThrow(() -> new BusinessException(
                            "Bac ou sac infecté introuvable : " + codeContenant));

            if (bac.getEtat() != null && !ETATS_BAC_AUTORISES.contains(bac.getEtat())) {
                throw new BusinessException(
                        "Le bac " + bac.getCode() + " n'est pas disponible (état : "
                                + bac.getEtat() + ").");
            }
        }

        boolean infecte = sac != null || bac.getTypebac() == TypeBac.INFECTE;

        // ---- Validation de tous les linges avant toute écriture ----
        List<Linge> aAjouter = new ArrayList<>();
        List<String> erreurs = new ArrayList<>();

        for (String code : codes) {

            Linge linge = lingeRepo.findByCodeBarre(code)
                    .or(() -> lingeRepo.findByBarcodeOrRfid(code))
                    .orElse(null);

            if (linge == null) {
                erreurs.add(code + " : linge introuvable");
                continue;
            }

            if (collecteLingeRepo.existsByCollecte_IdAndLinge_Id(collecteId, linge.getId())) {
                erreurs.add(code + " : déjà dans cette collecte");
                continue;
            }

            if (linge.getBac() != null || linge.getSacInfecte() != null) {
                erreurs.add(code + " : déjà dans un bac ou un sac");
                continue;
            }

            if (linge.getEtat() != null && !ETATS_LINGE_AUTORISES.contains(linge.getEtat())) {
                erreurs.add(code + " : état non autorisé (" + linge.getEtat() + ")");
                continue;
            }

            boolean uniforme = linge.getCategorie() == CategLinge.UNIFORME;
            if (nature == Nature.UNIFORME && !uniforme) {
                erreurs.add(code + " : n'est pas un uniforme");
                continue;
            }
            if (nature != Nature.UNIFORME && uniforme) {
                erreurs.add(code + " : un uniforme ne peut pas être ajouté à une collecte "
                        + (nature == Nature.CHAMBRE ? "de chambre" : "de service"));
                continue;
            }

            if (nature != Nature.UNIFORME
                    && collecte.getService() != null
                    && linge.getServiceId() != null
                    && !Objects.equals(linge.getServiceId(), collecte.getService())) {
                erreurs.add(code + " : appartient à un autre service");
                continue;
            }

            aAjouter.add(linge);
        }

        if (!erreurs.isEmpty()) {
            throw new BusinessException(
                    "Ajout refusé, aucun linge n'a été ajouté. " + String.join(" ; ", erreurs));
        }

        // ---- Capacité du bac ----
        if (bac != null) {
            int nbActuel = lingeRepo.findByBacId(bac.getId()).size();
            int capacite = bac.getCapacite_max() != null
                    ? bac.getCapacite_max().intValue()
                    : Integer.MAX_VALUE;
            int restant = Math.max(0, capacite - nbActuel);

            if (aAjouter.size() > restant) {
                throw new BusinessException(
                        "Bac " + bac.getCode() + " insuffisant : " + aAjouter.size()
                                + " linge(s) à mettre, " + restant
                                + " place(s) restante(s). Choisissez un autre bac.");
            }
        }

        // ---- Écriture ----
        EtatLinge nouvelEtat = infecte ? EtatLinge.INFECTE_COLLECTE : EtatLinge.COLLECTE;
        StatutCollecteLinge statut = infecte
                ? StatutCollecteLinge.INFECTE_PRESUME
                : StatutCollecteLinge.BLANC;
        TypeCollecte typeCollecte = infecte ? TypeCollecte.INFECTE : TypeCollecte.BLANC;
        String codeContenantFinal = sac != null ? sac.getNumeroSac() : bac.getCode();

        // Collecte de service + sac infecté : la quantité est déjà portée par le
        // sac (cloturer() additionne les lignes INFECTE ET les quantités des sacs).
        // On ne crée donc pas de CollecteLinge pour éviter de compter deux fois.
        boolean creerLigneCollecte = !(nature == Nature.SERVICE && sac != null);

        int horsAttendus = 0;
        Map<CategLinge, Integer> ajoutsSnapshot = new EnumMap<>(CategLinge.class);
        List<String> codesAjoutes = new ArrayList<>();

        for (Linge linge : aAjouter) {

            boolean dejaAttendu = estAttendu(nature, collecte, linge);
            if (!dejaAttendu) {
                horsAttendus++;
                if (linge.getCategorie() != null) {
                    ajoutsSnapshot.merge(linge.getCategorie(), 1, Integer::sum);
                }
            }

            EtatLinge etatAvant = linge.getEtat();

            if (sac != null) {
                linge.setSacInfecte(sac);
                sacInfecteService.ajouterLingeAuSac(sac, linge);
            } else {
                linge.setBac(bac);
            }

            linge.setEtat(nouvelEtat);
            linge.setStatutCollecte(statut);
            linge.setTypeCollecte(typeCollecte);
            linge.setLitIdSqlServer(null);
            linge.setChambreIdSqlServer(null);
            linge.setArsenalEmplacementID(null);
            linge.setEmplacement(null);
            lingeRepo.save(linge);

            if (creerLigneCollecte) {
                collecteLingeRepo.save(
                        CollecteLinge.builder()
                                .collecte(collecte)
                                .linge(linge)
                                .typeCollecte(typeCollecte)
                                .numeroSacInfecte(infecte ? codeContenantFinal : null)
                                .dateScan(LocalDateTime.now())
                                .build());
            }

            historiqueRepo.save(
                    HistoriqueLinge.builder()
                            .linge(linge)
                            .etatAvant(etatAvant)
                            .etatApres(nouvelEtat)
                            .personnelId(personnelId)
                            .typeOperation(infecte
                                    ? TypeOperation.DECLARATION_SAC_INFECTE
                                    : TypeOperation.COLLECTE)
                            .dateOperation(LocalDateTime.now())
                            .collecte(collecte)
                            .observations("AJOUT MANUEL (administrateur) "
                                    + (infecte ? "INFECTE" : "BLANC")
                                    + " contenant=" + codeContenantFinal)
                            .build());

            codesAjoutes.add(linge.getCodeBarre());
        }

        // ---- Compteurs de la collecte ----
        if (horsAttendus > 0) {
            int attendus = collecte.getNbAttendus() != null ? collecte.getNbAttendus() : 0;
            collecte.setNbAttendus(attendus + horsAttendus);

            if (nature == Nature.SERVICE) {
                majSnapshotQuantites(collecte, ajoutsSnapshot);
            }
        }

        // Collecte déjà clôturée côté agent (dateFin renseignée) mais pas encore
        // remise : les totaux ont été figés, on les recalcule.
        if (collecte.getDateFin() != null) {
            recalculerTotaux(collecte, nature);
        }

        collecteRepo.save(collecte);

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("collecteId", collecteId);
        reponse.put("typeDeCollecte", nature.name());
        reponse.put("codeContenant", codeContenantFinal);
        reponse.put("typeContenant", sac != null
                ? "SAC_INFECTE"
                : infecte ? "BAC_INFECTE" : "BAC_BLANC");
        reponse.put("estInfecte", infecte);
        reponse.put("nbAjoutes", aAjouter.size());
        reponse.put("nbHorsAttendus", horsAttendus);
        reponse.put("nbAttendus", collecte.getNbAttendus());
        reponse.put("codes", codesAjoutes);
        return reponse;
    }


    private Nature natureDe(Collecte collecte) {
        if (collecte.getChambreIdSqlServer() != null) {
            return Nature.CHAMBRE;
        }
        if (collecte.getNumeroCollecte() != null
                && collecte.getNumeroCollecte().startsWith("UNI-")) {
            return Nature.UNIFORME;
        }
        return Nature.SERVICE;
    }

    /**
     * Un linge « attendu » est déjà compté dans nbAttendus à l'ouverture de la
     * collecte : il ne faut pas l'ajouter une seconde fois.
     */
    private boolean estAttendu(Nature nature, Collecte collecte, Linge linge) {
        if (linge.getEtat() != EtatLinge.LIVRE) {
            return false;
        }
        switch (nature) {
            case CHAMBRE:
                return collecte.getChambreIdSqlServer() != null
                        && Objects.equals(linge.getChambreIdSqlServer(),
                        collecte.getChambreIdSqlServer());
            case SERVICE:
                return collecte.getService() != null
                        && Objects.equals(linge.getServiceId(), collecte.getService());
            default:
                return true; // uniforme : le porteur n'est pas conservé sur la collecte
        }
    }

    private void majSnapshotQuantites(Collecte collecte, Map<CategLinge, Integer> ajouts) {
        String json = collecte.getQuantitesAttendues();
        if (json == null || json.isBlank() || ajouts.isEmpty()) {
            return;
        }
        try {
            Map<CategLinge, Integer> snapshot = objectMapper.readValue(
                    json, new TypeReference<EnumMap<CategLinge, Integer>>() {});
            ajouts.forEach((cat, n) -> snapshot.merge(cat, n, Integer::sum));
            collecte.setQuantitesAttendues(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "Impossible de mettre à jour le snapshot des quantités attendues.");
        }
    }

    private void recalculerTotaux(Collecte collecte, Nature nature) {
        List<CollecteLinge> lignes = collecteLingeRepo.findByCollecte_Id(collecte.getId());

        long blancs = compter(lignes, TypeCollecte.BLANC);
        long infectes = compter(lignes, TypeCollecte.INFECTE);
        long propres = compter(lignes, TypeCollecte.PROPRE_EN_PLACE);
        long manquants = compter(lignes, TypeCollecte.MANQUANT);

        if (nature == Nature.CHAMBRE) {
            long attendus = collecte.getNbAttendus() != null ? collecte.getNbAttendus() : 0;
            propres = Math.max(0, attendus - blancs - infectes - manquants);
        } else if (nature == Nature.SERVICE) {
            infectes += sacInfecteRepo.findByCollecte_Id(collecte.getId()).stream()
                    .mapToLong(s -> s.getItems() == null
                            ? 0L
                            : s.getItems().stream()
                              .mapToLong(i -> i.getQuantiteDeclaree() == null
                                              ? 0L
                                              : i.getQuantiteDeclaree())
                              .sum())
                    .sum();
        }

        collecte.setNbScannésBlanc((int) blancs);
        collecte.setNbInfectésPrésumés((int) infectes);
        collecte.setNbPropres((int) propres);
        collecte.setNbManquants((int) manquants);
    }

    private long compter(List<CollecteLinge> lignes, TypeCollecte type) {
        return lignes.stream().filter(l -> l.getTypeCollecte() == type).count();
    }
}