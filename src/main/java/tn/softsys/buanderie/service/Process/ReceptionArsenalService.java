
package tn.softsys.buanderie.service.Process;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import tn.softsys.buanderie.Sqlserver.Cubix.ArsenalCubixService;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixPersonnelService;
import tn.softsys.buanderie.Sqlserver.dtos.ArsenalDto;
import tn.softsys.buanderie.Sqlserver.dtos.PersonnelDto;
import tn.softsys.buanderie.Sqlserver.dtos.EmplacementDto;
import tn.softsys.buanderie.dto.ReceptionArsenalRequestDTO;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.EtatLivraison;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.repository.*;

@Service
@RequiredArgsConstructor
public class ReceptionArsenalService {

    private final RemiseLingeRepository remiseLingeRepository;
    private final RemiseLingeItemRepository remiseLingeItemRepository;
    private final ReceptionArsenalRepository receptionArsenalRepository;
    private final ReceptionArsenalItemRepository receptionArsenalItemRepository;
    private final ReceptionArsenalEmplacementRepository receptionArsenalEmplacementRepository;
    private final StockArsenalRepository stockArsenalRepository;
    private final ArsenalCubixService arsenalCubixService;
    private final CubixPersonnelService personnelService;
    private final RemiseLingeLingeRepository remiseLingeLingeRepository;
    private final LingeRepository lingeRepository;
    private final LingeArsenalRepository lingeArsenalRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;

    @Transactional
    public ReceptionArsenal enregistrerReception(
            ReceptionArsenalRequestDTO request) {

        // 1. Vérifier la remise
        RemiseLinge remise = remiseLingeRepository
                .findById(request.getRemiseLingeId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Remise introuvable."));

        Long arsenalID = request.getArsenalID();

        // 2. Une remise doit rester dans le même Arsenal
        List<ReceptionArsenal> anciennesReceptions =
                receptionArsenalRepository
                        .findByRemiseLinge_IdOrderByDateReceptionDesc(
                                remise.getId());

        for (ReceptionArsenal ancienne : anciennesReceptions) {
            if (!ancienne.getArsenalID().equals(arsenalID)) {
                throw new IllegalArgumentException(
                        "Cette remise est déjà associée à un autre Arsenal.");
            }
        }

        // 3. Charger les emplacements autorisés de cet Arsenal
        List<EmplacementDto> emplacements =
                arsenalCubixService.getEmplacements(arsenalID);

        if (emplacements == null || emplacements.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucun emplacement trouvé pour cet Arsenal.");
        }

        Set<Long> emplacementIDs = emplacements.stream()
                .map(EmplacementDto::getEmplacementID)
                .collect(Collectors.toSet());

        // 4. Charger les articles de la remise
        List<RemiseLingeItem> articlesRemise =
                remiseLingeItemRepository.findByRemiseLinge(remise);

        Map<CategLinge, RemiseLingeItem> articlesParCategorie =
                articlesRemise.stream().collect(Collectors.toMap(
                        RemiseLingeItem::getCategorie,
                        Function.identity()
                ));

        // 5. Vérifier les catégories et les quantités
        Set<CategLinge> categoriesTraitees = new HashSet<>();

        for (ReceptionArsenalRequestDTO.ArticleRecuDTO article
                : request.getArticles()) {

            CategLinge categorie = article.getCategorie();
            Integer quantiteRecue = article.getQuantiteRecue();

            if (!categoriesTraitees.add(categorie)) {
                throw new IllegalArgumentException(
                        "La catégorie " + categorie
                                + " apparaît plusieurs fois dans la réception.");
            }

            if (quantiteRecue == null || quantiteRecue <= 0) {
                throw new IllegalArgumentException(
                        "La quantité reçue doit être supérieure à zéro.");
            }

            RemiseLingeItem articleRemise =
                    articlesParCategorie.get(categorie);

            if (articleRemise == null) {
                throw new IllegalArgumentException(
                        "La catégorie " + categorie
                                + " ne figure pas dans cette remise.");
            }

            int quantiteDejaRecue = 0;

            for (ReceptionArsenal ancienne : anciennesReceptions) {
                List<ReceptionArsenalItem> anciensArticles =
                        receptionArsenalItemRepository
                                .findByReceptionArsenal_Id(ancienne.getId());

                quantiteDejaRecue += anciensArticles.stream()
                        .filter(a -> a.getCategorie() == categorie)
                        .mapToInt(ReceptionArsenalItem::getQuantiteRecue)
                        .sum();
            }

            int quantiteAttendue = articleRemise.getQuantiteRemise();
            int resteARecevoir = quantiteAttendue - quantiteDejaRecue;

            if (quantiteRecue > resteARecevoir) {
                throw new IllegalArgumentException(
                        "Quantité trop élevée pour " + categorie
                                + ". Reste à recevoir : " + resteARecevoir);
            }

            // 6. Vérifier la répartition dans les emplacements
            if (article.getRangements() == null
                    || article.getRangements().isEmpty()) {
                throw new IllegalArgumentException(
                        "Veuillez sélectionner un emplacement pour "
                                + categorie + ".");
            }

            int totalRange = 0;

            for (ReceptionArsenalRequestDTO.RangementDTO rangement
                    : article.getRangements()) {

                if (rangement.getEmplacementID() == null
                        || !emplacementIDs.contains(
                        rangement.getEmplacementID())) {
                    throw new IllegalArgumentException(
                            "L'emplacement "
                                    + rangement.getEmplacementID()
                                    + " n'appartient pas à cet Arsenal.");
                }

                if (rangement.getQuantiteRangee() == null
                        || rangement.getQuantiteRangee() <= 0) {
                    throw new IllegalArgumentException(
                            "La quantité rangée doit être supérieure à zéro.");
                }

                totalRange += rangement.getQuantiteRangee();
            }

            if (totalRange != quantiteRecue) {
                throw new IllegalArgumentException(
                        "Pour " + categorie
                                + ", la somme des quantités rangées ("
                                + totalRange
                                + ") doit être égale à la quantité reçue ("
                                + quantiteRecue + ").");
            }
        }

        // 7. Créer l'enregistrement du passage de réception
        ReceptionArsenal reception = ReceptionArsenal.builder()
                .remiseLinge(remise)
                .arsenalID(arsenalID)
                .statutReception("VALIDEE")
                .build();

        reception = receptionArsenalRepository.save(reception);

        // 8. Enregistrer les quantités reçues et leur rangement
        for (ReceptionArsenalRequestDTO.ArticleRecuDTO article
                : request.getArticles()) {

            ReceptionArsenalItem receptionItem =
                    ReceptionArsenalItem.builder()
                            .receptionArsenal(reception)
                            .categorie(article.getCategorie())
                            .quantiteRecue(article.getQuantiteRecue())
                            .build();

            receptionItem =
                    receptionArsenalItemRepository.save(receptionItem);

            for (ReceptionArsenalRequestDTO.RangementDTO rangement
                    : article.getRangements()) {

                ReceptionArsenalEmplacement receptionEmplacement =
                        ReceptionArsenalEmplacement.builder()
                                .receptionArsenalItem(receptionItem)
                                .emplacementID(rangement.getEmplacementID())
                                .quantiteRangee(rangement.getQuantiteRangee())
                                .build();

                receptionArsenalEmplacementRepository.save(
                        receptionEmplacement);

                // 9. Mettre à jour le stock de cet emplacement
                StockArsenal stock = stockArsenalRepository
                        .rechercherStock(
                                arsenalID,
                                rangement.getEmplacementID(),
                                article.getCategorie())
                        .orElseGet(() -> StockArsenal.builder()
                                .arsenalID(arsenalID)
                                .emplacementID(rangement.getEmplacementID())
                                .categorie(article.getCategorie())
                                .quantite(0)
                                .build());

                stock.setQuantite(
                        stock.getQuantite() + rangement.getQuantiteRangee());

                stockArsenalRepository.save(stock);
            }
        }

        return reception;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRemisesAgent(String badgeAgent) {
        PersonnelDto agent = getAgent(badgeAgent);

        List<Map<String, Object>> remises = new ArrayList<>();

        for (RemiseLinge remise : remiseLingeRepository
                .findByPersonnelIdAndEtatNotOrderByDateLivraisonDesc(agent.getId(), EtatLivraison.ANNULEE)) {

            List<RemiseLingeLinge> aRanger = remiseLingeLingeRepository.findARanger(remise.getId());
            if (aRanger.isEmpty()) {
                continue;
            }

            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("remiseLingeId", remise.getId());
            ligne.put("numeroLivraison", remise.getNumeroLivraison());
            ligne.put("serviceId", remise.getService());
            ligne.put("dateLivraison", remise.getDateLivraison());
            ligne.put("totalARanger", aRanger.size());
            ligne.put("parCategorie", compterParCategorie(aRanger));
            remises.add(ligne);
        }

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("agentId", agent.getId());
        reponse.put("agentNom", agent.getNomPrenom());
        reponse.put("remises", remises);
        return reponse;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLingesARanger(Long remiseLingeId) {
        RemiseLinge remise = remiseLingeRepository.findById(remiseLingeId)
                .orElseThrow(() -> new IllegalArgumentException("Remise introuvable."));

        List<RemiseLingeLinge> aRanger = remiseLingeLingeRepository.findARanger(remiseLingeId);

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("remiseLingeId", remise.getId());
        reponse.put("numeroLivraison", remise.getNumeroLivraison());
        reponse.put("totalARanger", aRanger.size());
        reponse.put("parCategorie", compterParCategorie(aRanger));
        reponse.put("linges", aRanger.stream().map(r -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("lingeId", r.getLinge().getId());
            l.put("codeBarre", r.getLinge().getCodeBarre());
            l.put("designation", r.getLinge().getDesignation());
            l.put("categorie", r.getCategorie().name());
            return l;
        }).toList());
        return reponse;
    }

    @Transactional
    public Map<String, Object> rangerTout(Long remiseLingeId, Long arsenalID, String emplacement,
                                          Integer quantiteComptee, String badgeAgent) {
        RemiseLinge remise = remiseLingeRepository.findById(remiseLingeId)
                .orElseThrow(() -> new IllegalArgumentException("Remise introuvable."));
        EmplacementDto cible = resoudreEmplacement(arsenalID, emplacement);
        Long personnelId = badgeAgent != null && !badgeAgent.isBlank()
                ? getAgent(badgeAgent).getId()
                : remise.getPersonnelId();

        List<RemiseLingeLinge> aRanger = remiseLingeLingeRepository.findARanger(remiseLingeId);
        if (aRanger.isEmpty()) {
            throw new IllegalArgumentException("Tous les linges de cette remise sont déjà rangés.");
        }

        if (quantiteComptee == null || quantiteComptee != aRanger.size()) {
            throw new IllegalArgumentException("Quantité non conforme : " + quantiteComptee
                    + " comptée(s) pour " + aRanger.size() + " attendue(s). Rangez les linges un par un.");
        }

        String nomArsenal = nomArsenal(arsenalID);
        aRanger.forEach(r -> ranger(r, arsenalID, cible, personnelId, nomArsenal, remise.getNumeroLivraison()));

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("message", aRanger.size() + " linge(s) rangé(s) dans " + libelle(cible));
        reponse.put("remiseLingeId", remiseLingeId);
        reponse.put("arsenalID", arsenalID);
        reponse.put("arsenalEmplacementID", cible.getEmplacementID());
        reponse.put("nbRanges", aRanger.size());
        reponse.put("resteARanger", 0);
        return reponse;
    }

    @Transactional
    public Map<String, Object> rangerLinge(Long remiseLingeId, Long arsenalID, String emplacement,
                                           String codeLinge, String badgeAgent) {
        RemiseLinge remise = remiseLingeRepository.findById(remiseLingeId)
                .orElseThrow(() -> new IllegalArgumentException("Remise introuvable."));
        EmplacementDto cible = resoudreEmplacement(arsenalID, emplacement);
        Long personnelId = badgeAgent != null && !badgeAgent.isBlank()
                ? getAgent(badgeAgent).getId()
                : remise.getPersonnelId();

        Linge linge = rechercherLinge(codeLinge);

        RemiseLingeLinge lien = remiseLingeLingeRepository
                .findByRemiseLinge_IdAndLinge_Id(remiseLingeId, linge.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Le linge " + linge.getCodeBarre() + " ne fait pas partie de la remise "
                                + remise.getNumeroLivraison() + "."));

        if (lien.getDateRangement() != null) {
            throw new IllegalArgumentException("Le linge " + linge.getCodeBarre() + " est déjà rangé.");
        }

        ranger(lien, arsenalID, cible, personnelId, nomArsenal(arsenalID), remise.getNumeroLivraison());

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("message", linge.getCodeBarre() + " rangé dans " + libelle(cible));
        reponse.put("codeBarre", linge.getCodeBarre());
        reponse.put("categorie", lien.getCategorie().name());
        reponse.put("arsenalEmplacementID", cible.getEmplacementID());
        reponse.put("resteARanger", remiseLingeLingeRepository.countByRemiseLinge_IdAndDateRangementIsNull(remiseLingeId));
        return reponse;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStockArsenal(Long arsenalID) {
        List<EmplacementDto> emplacements = arsenalCubixService.getEmplacements(arsenalID);
        if (emplacements == null) {
            emplacements = List.of();
        }

        Set<Long> ids = emplacements.stream()
                .map(EmplacementDto::getEmplacementID)
                .collect(Collectors.toSet());

        List<Linge> linges = ids.isEmpty()
                ? List.of()
                : lingeArsenalRepository.findByArsenalEmplacementIDInAndEtatOrderByCategorieAsc(ids, EtatLinge.STOCKE);

        Map<Long, List<Linge>> parEmplacement = linges.stream()
                .collect(Collectors.groupingBy(Linge::getArsenalEmplacementID));

        Map<String, Long> totalParCategorie = new TreeMap<>();
        linges.forEach(l -> totalParCategorie.merge(nomCategorie(l), 1L, Long::sum));

        List<Map<String, Object>> lignes = new ArrayList<>();
        for (EmplacementDto e : emplacements) {
            List<Linge> contenu = parEmplacement.getOrDefault(e.getEmplacementID(), List.of());
            Map<String, Long> quantites = new TreeMap<>();
            contenu.forEach(l -> quantites.merge(nomCategorie(l), 1L, Long::sum));

            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("arsenalEmplacementID", e.getEmplacementID());
            ligne.put("nom", libelle(e));
            ligne.put("codeBarre", e.getCodeBarre());
            ligne.put("total", contenu.size());
            ligne.put("quantitesParCategorie", quantites);
            lignes.add(ligne);
        }

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("arsenalID", arsenalID);
        reponse.put("arsenal", nomArsenal(arsenalID));
        reponse.put("total", linges.size());
        reponse.put("totalParCategorie", totalParCategorie);
        reponse.put("emplacements", lignes);
        return reponse;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getContenuEmplacement(Long arsenalEmplacementID) {
        return lingeArsenalRepository
                .findByArsenalEmplacementIDAndEtatOrderByCategorieAsc(arsenalEmplacementID, EtatLinge.STOCKE)
                .stream()
                .map(l -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put("lingeId", l.getId());
                    ligne.put("codeBarre", l.getCodeBarre());
                    ligne.put("designation", l.getDesignation());
                    ligne.put("categorie", nomCategorie(l));
                    return ligne;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> localiserLinge(String codeLinge) {
        Linge linge = rechercherLinge(codeLinge);

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("codeBarre", linge.getCodeBarre());
        reponse.put("categorie", nomCategorie(linge));
        reponse.put("etat", linge.getEtat() != null ? linge.getEtat().name() : null);

        if (linge.getArsenalEmplacementID() == null) {
            reponse.put("localise", false);
            reponse.put("message", "Ce linge n'est rangé dans aucun arsenal.");
            return reponse;
        }

        EmplacementDto emplacement = arsenalCubixService
                .getEmplacementById(linge.getArsenalEmplacementID())
                .orElse(null);

        reponse.put("localise", true);
        reponse.put("arsenalEmplacementID", linge.getArsenalEmplacementID());
        reponse.put("emplacement", emplacement != null
                ? libelle(emplacement)
                : "Emplacement " + linge.getArsenalEmplacementID());
        reponse.put("arsenalID", emplacement != null ? emplacement.getArsenalID() : null);
        reponse.put("arsenal", emplacement != null ? nomArsenal(emplacement.getArsenalID()) : null);
        return reponse;
    }

    private void ranger(RemiseLingeLinge lien, Long arsenalID, EmplacementDto cible, Long personnelId,
                        String nomArsenal, String numeroLivraison) {
        LocalDateTime maintenant = LocalDateTime.now();
        Linge linge = lien.getLinge();
        EtatLinge avant = linge.getEtat();

        linge.setArsenalEmplacementID(cible.getEmplacementID());
        linge.setEtat(EtatLinge.STOCKE);
        lingeRepository.save(linge);

        lien.setDateRangement(maintenant);
        remiseLingeLingeRepository.save(lien);

        historiqueLingeRepository.save(
                HistoriqueLinge.builder()
                        .linge(linge)
                        .personnelId(personnelId)
                        .etatAvant(avant)
                        .etatApres(EtatLinge.STOCKE)
                        .typeOperation(TypeOperation.MISE_EN_STOCKAGE)
                        .dateOperation(maintenant)
                        .observations("Rangé dans " + nomArsenal + " / " + libelle(cible)
                                + " — remise " + numeroLivraison)
                        .build()
        );
    }

    private Map<String, Long> compterParCategorie(List<RemiseLingeLinge> liens) {
        Map<String, Long> parCategorie = new TreeMap<>();
        liens.forEach(r -> parCategorie.merge(r.getCategorie().name(), 1L, Long::sum));
        return parCategorie;
    }

    private EmplacementDto resoudreEmplacement(Long arsenalID, String emplacement) {
        if (arsenalID == null) {
            throw new IllegalArgumentException("Veuillez choisir un arsenal.");
        }
        if (emplacement == null || emplacement.isBlank()) {
            throw new IllegalArgumentException("Veuillez choisir ou scanner un emplacement.");
        }
        String valeur = emplacement.trim();
        return arsenalCubixService.getEmplacements(arsenalID).stream()
                .filter(e -> valeur.equals(e.getCodeBarre()) || valeur.equals(String.valueOf(e.getEmplacementID())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "L'emplacement " + valeur + " n'appartient pas à cet arsenal."));
    }

    private Linge rechercherLinge(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Veuillez scanner un linge.");
        }
        String valeur = code.trim();
        return lingeRepository.findByCodeBarre(valeur)
                .orElseGet(() -> {
                    Linge parTag = lingeRepository.findByRfidTag(valeur);
                    if (parTag == null) {
                        throw new IllegalArgumentException("Linge introuvable : " + valeur);
                    }
                    return parTag;
                });
    }

    private PersonnelDto getAgent(String badge) {
        if (badge == null || badge.isBlank()) {
            throw new IllegalArgumentException("Veuillez scanner le badge de l'agent.");
        }
        try {
            PersonnelDto agent = personnelService.getByCode(badge.trim());
            if (agent == null || agent.getId() == null) {
                throw new IllegalArgumentException("Agent introuvable : " + badge);
            }
            return agent;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Agent introuvable : " + badge);
        }
    }

    private String nomArsenal(Long arsenalID) {
        try {
            return arsenalCubixService.getArsenaux().stream()
                    .filter(a -> Objects.equals(a.getArsenalID(), arsenalID))
                    .map(ArsenalDto::getDesignation)
                    .findFirst()
                    .orElse("Arsenal " + arsenalID);
        } catch (RuntimeException e) {
            return "Arsenal " + arsenalID;
        }
    }

    private String libelle(EmplacementDto e) {
        String nom = e.getNom() != null ? e.getNom() : "Emplacement " + e.getEmplacementID();
        return e.getNumero() != null ? nom + " (n° " + e.getNumero() + ")" : nom;
    }

    private String nomCategorie(Linge linge) {
        return linge.getCategorie() != null ? linge.getCategorie().name() : "AUTRE";
    }
}
