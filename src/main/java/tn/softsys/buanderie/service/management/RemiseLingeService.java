package tn.softsys.buanderie.service.management;

import tn.softsys.buanderie.repository.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.service.referentiel.PersonnelReferentielService;
import tn.softsys.buanderie.dto.PersonnelDto;
import tn.softsys.buanderie.dto.Collectes.ResumeCollecteDTO;
import tn.softsys.buanderie.dto.ResumeCategorieDTO;
import tn.softsys.buanderie.dto.StatutCategorieDTO;
import tn.softsys.buanderie.dto.StatutCollecteDTO;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatCollecte;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.EtatLivraison;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.CollecteRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.RemiseLingeCollecteRepository;
import tn.softsys.buanderie.repository.RemiseLingeItemRepository;
import tn.softsys.buanderie.repository.RemiseLingeLingeRepository;
import tn.softsys.buanderie.repository.RemiseLingeRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RemiseLingeService {

    private final RemiseLingeRepository remiseLingeRepository;
    private final RemiseLingeItemRepository remiseLingeItemRepository;
    private final RemiseLingeLingeRepository remiseLingeLingeRepository;
    private final RemiseLingeCollecteRepository remiseLingeCollecteRepository;
    private final CollecteRepository collecteRepository;
    private final LingeRepository lingeRepository;
    private final ServiceRepository serviceRepository;
    private final CollecteResumeService collecteService;
    private final PersonnelReferentielService personnelService;

    @Transactional
    public Map<String, Object> getCollectesNonCloturees(Long personnelId) {

        List<Map<String, Object>> collectes = new ArrayList<>();
        Map<Long, Map<CategLinge, Integer>> totalParService = new LinkedHashMap<>();

        for (Collecte collecte : collectesNonClotureesAvecReste(personnelId)) {

            Map<CategLinge, Integer> restes = calculerRestes(collecte.getId());

            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("collecteId", collecte.getId());
            ligne.put("numeroCollecte", collecte.getNumeroCollecte());
            ligne.put("serviceId", collecte.getService());
            ligne.put("codeChambre", collecte.getCodeChambre());
            ligne.put("dateCollecte", collecte.getDateCollecte());
            ligne.put("resteParCategorie", versLibelles(restes));
            ligne.put("totalReste", restes.values().stream().mapToInt(Integer::intValue).sum());
            collectes.add(ligne);

            Map<CategLinge, Integer> cumul =
                    totalParService.computeIfAbsent(collecte.getService(), k -> new EnumMap<>(CategLinge.class));
            restes.forEach((categorie, quantite) -> cumul.merge(categorie, quantite, Integer::sum));
        }

        List<Map<String, Object>> services = new ArrayList<>();
        totalParService.forEach((serviceId, cumul) -> {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("serviceId", serviceId);
            ligne.put("resteParCategorie", versLibelles(cumul));
            ligne.put("totalReste", cumul.values().stream().mapToInt(Integer::intValue).sum());
            services.add(ligne);
        });

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("personnelId", personnelId);
        reponse.put("nbCollectes", collectes.size());
        reponse.put("services", services);
        reponse.put("collectes", collectes);
        return reponse;
    }

    @Transactional
    public RemiseLinge creerRemise(
            String matriculeAgentRemise,
            Long id
    ) {
        return creerRemise(matriculeAgentRemise, id, null);
    }

    @Transactional
    public RemiseLinge creerRemise(
            String matriculeAgentRemise,
            Long id,
            Long serviceIdChoisi
    ) {
        PersonnelDto personnelRemise =
                personnelService.getByCode(matriculeAgentRemise);

        if (personnelRemise == null) {
            throw new ResourceNotFoundException(
                    "Personnel",
                    "codeBarre",
                    matriculeAgentRemise
            );
        }

        List<Collecte> collectesOuvertes = collectesNonClotureesAvecReste(id);

        if (collectesOuvertes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Collecte non clôturée",
                    "matricule agent",
                    id
            );
        }

        Long serviceId = serviceIdChoisi != null
                ? serviceIdChoisi
                : collectesOuvertes.get(0).getService();

        List<Collecte> collectes = collectesOuvertes.stream()
                .filter(c -> Objects.equals(c.getService(), serviceId))
                .toList();

        if (collectes.isEmpty()) {
            throw new RuntimeException(
                    "Aucune collecte non clôturée de cet agent pour le service " + serviceId + "."
            );
        }

        if (serviceRepository.findById(serviceId).isEmpty()) {
            throw new ResourceNotFoundException(
                    "Service",
                    "id",
                    serviceId
            );
        }

        Map<CategLinge, Integer> resteTotal = new EnumMap<>(CategLinge.class);
        for (Collecte collecte : collectes) {
            calculerRestes(collecte.getId())
                    .forEach((categorie, reste) -> resteTotal.merge(categorie, reste, Integer::sum));
        }

        if (resteTotal.values().stream().mapToInt(Integer::intValue).sum() <= 0) {
            throw new RuntimeException(
                    "Les collectes de cet agent ne contiennent aucun linge à remettre."
            );
        }

        RemiseLinge remise =
                RemiseLinge.builder()
                        .collecte(collectes.get(0))
                        .service(serviceId)
                        .personnelId(personnelRemise.getId())
                        .dateLivraison(LocalDateTime.now())
                        .etat(EtatLivraison.EN_COURS)
                        .build();

        remise = remiseLingeRepository.save(remise);

        for (Collecte collecte : collectes) {
            remiseLingeCollecteRepository.save(
                    RemiseLingeCollecte.builder()
                            .remiseLinge(remise)
                            .collecte(collecte)
                            .build()
            );
        }

        for (Map.Entry<CategLinge, Integer> entry : resteTotal.entrySet()) {

            if (entry.getValue() <= 0) {
                continue;
            }

            RemiseLingeItem item =
                    RemiseLingeItem.builder()
                            .remiseLinge(remise)
                            .categorie(entry.getKey())
                            .quantiteCollectee(entry.getValue())
                            .quantiteDisponible(
                                    getStockDisponible(serviceId, entry.getKey())
                            )
                            .quantiteRemise(0)
                            .build();

            remiseLingeItemRepository.save(item);
        }

        mettreAJourEtatRemise(remise);

        return remise;
    }

    private List<Collecte> collectesNonClotureesAvecReste(Long personnelId) {
        return collecteRepository
                .findByPersonnelIdAndEtatCollecteNotOrderByDateCollecteAscIdAsc(
                        personnelId,
                        EtatCollecte.CLOTUREE
                )
                .stream()
                .filter(c -> c.getService() != null)
                .filter(c -> calculerRestes(c.getId()).values().stream().anyMatch(q -> q > 0))
                .toList();
    }

    private Map<CategLinge, Integer> calculerRestes(Long collecteId) {

        Map<CategLinge, Integer> restes = new EnumMap<>(CategLinge.class);

        ResumeCollecteDTO resume = collecteService.getResume(collecteId);

        if (resume == null || resume.getCategories() == null) {
            return restes;
        }

        Map<CategLinge, Integer> remis = getQuantitesRemisesParCategorie(collecteId);

        for (ResumeCategorieDTO categorieDTO : resume.getCategories()) {

            if (categorieDTO.getCategorie() == null || categorieDTO.getQuantite() == null) {
                continue;
            }

            int reste = categorieDTO.getQuantite()
                    - remis.getOrDefault(categorieDTO.getCategorie(), 0);

            if (reste > 0) {
                restes.merge(categorieDTO.getCategorie(), reste, Integer::sum);
            }
        }

        return restes;
    }

    private Map<CategLinge, Integer> getQuantitesRemisesParCategorie(Long collecteId) {

        Map<CategLinge, Integer> remis = new EnumMap<>(CategLinge.class);

        for (Object[] row : remiseLingeLingeRepository.countRemisParCategoriePourCollecte(collecteId)) {
            remis.put((CategLinge) row[0], ((Number) row[1]).intValue());
        }

        return remis;
    }

    private Integer getQuantiteDejaRemise(
            Long collecteId,
            CategLinge categorie
    ) {
        return (int) remiseLingeLingeRepository.countRemisPourCollecte(collecteId, categorie);
    }

    private Map<String, Integer> versLibelles(Map<CategLinge, Integer> quantites) {
        Map<String, Integer> resultat = new TreeMap<>();
        quantites.forEach((categorie, quantite) -> resultat.put(categorie.name(), quantite));
        return resultat;
    }

    private Integer getStockDisponible(
            Long serviceId,
            CategLinge categorie
    ) {
        long stock =
                lingeRepository
                        .countByServiceIdAndCategorieAndEtat(
                                serviceId,
                                categorie,
                                EtatLinge.STOCKE
                        );

        return (int) stock;
    }

    private void mettreAJourEtatRemise(
            RemiseLinge remise
    ) {
        List<RemiseLingeItem> items =
                remiseLingeItemRepository
                        .findByRemiseLinge(remise);

        if (items == null || items.isEmpty()) {
            remise.setEtat(EtatLivraison.EN_COURS);
            remiseLingeRepository.save(remise);
            return;
        }

        boolean complet = true;
        boolean partiel = false;

        for (RemiseLingeItem item : items) {

            int aRemettre =
                    item.getQuantiteCollectee() == null
                            ? 0
                            : item.getQuantiteCollectee();

            int remis =
                    item.getQuantiteRemise() == null
                            ? 0
                            : item.getQuantiteRemise();

            if (remis < aRemettre) {
                complet = false;
            }

            if (remis > 0) {
                partiel = true;
            }
        }

        if (complet) {
            remise.setEtat(EtatLivraison.COMPLETEE);
        } else if (partiel) {
            remise.setEtat(EtatLivraison.PARTIELLE);
        } else {
            remise.setEtat(EtatLivraison.EN_ATTENTE);
        }

        remiseLingeRepository.save(remise);
    }

    @Transactional
    public List<RemiseLingeItem> getResteAremettre(
            Long collecteId
    ) {
        ResumeCollecteDTO resume =
                collecteService.getResume(collecteId);

        List<RemiseLingeItem> result =
                new ArrayList<>();

        Collecte collecte =
                collecteRepository.findById(collecteId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Collecte",
                                        "id",
                                        collecteId
                                )
                        );

        Long serviceId =
                collecte.getService();

        for (ResumeCategorieDTO categorieDTO :
                resume.getCategories()) {

            CategLinge categorie =
                    categorieDTO.getCategorie();

            int collectee =
                    categorieDTO.getQuantite();

            int dejaRemise =
                    getQuantiteDejaRemise(
                            collecteId,
                            categorie
                    );

            int reste =
                    Math.max(
                            0,
                            collectee - dejaRemise
                    );

            int stock =
                    getStockDisponible(
                            serviceId,
                            categorie
                    );

            int disponiblePourRemise =
                    Math.min(
                            reste,
                            stock
                    );

            RemiseLingeItem item =
                    RemiseLingeItem.builder()
                            .remiseLinge(null)
                            .categorie(categorie)
                            .quantiteCollectee(collectee)
                            .quantiteDisponible(stock)
                            .quantiteRemise(disponiblePourRemise)
                            .build();

            result.add(item);
        }

        return result;
    }

    public RemiseLinge getById(Long id) {
        return remiseLingeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "RemiseLinge",
                                "id",
                                id
                        )
                );
    }

    public List<RemiseLinge> getByCollecte(
            Long collecteId
    ) {
        Map<Long, RemiseLinge> remises = new LinkedHashMap<>();

        remiseLingeRepository
                .findByCollecte_IdOrderByDateLivraisonAsc(collecteId)
                .forEach(r -> remises.put(r.getId(), r));

        remiseLingeCollecteRepository
                .findRemisesDeCollecte(collecteId)
                .forEach(r -> remises.putIfAbsent(r.getId(), r));

        return new ArrayList<>(remises.values());
    }

    @Transactional
    public RemiseLinge terminerRemise(
            Long remiseId
    ) {
        RemiseLinge remise =
                remiseLingeRepository.findById(remiseId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "RemiseLinge",
                                        "id",
                                        remiseId
                                )
                        );

        mettreAJourEtatRemise(remise);

        return remiseLingeRepository.save(remise);
    }

    @Transactional
    public RemiseLingeItem scannerLinge(
            Long remiseId,
            String codeBarre
    ) {
        RemiseLinge remise = remiseLingeRepository.findById(remiseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "RemiseLinge",
                                "id",
                                remiseId
                        )
                );

        Linge linge = lingeRepository.findByCodeBarre(codeBarre)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "codeBarre",
                                codeBarre
                        )
                );

        if (linge.getEtat() == EtatLinge.LIVRE) {
            throw new RuntimeException(
                    "Ce linge est déjà livré : " + codeBarre
            );
        }

        if (!Objects.equals(
                linge.getServiceId(),
                remise.getService()
        )) {
            throw new RuntimeException(
                    "Ce linge n'appartient pas au service de cette remise."
            );
        }

        RemiseLingeItem item =
                remiseLingeItemRepository
                        .findByRemiseLingeAndCategorie(
                                remise,
                                linge.getCategorie()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Aucun item trouvé pour la catégorie : "
                                                + linge.getCategorie()
                                )
                        );

        int quantiteRestante =
                item.getQuantiteCollectee()
                        - item.getQuantiteRemise();

        if (quantiteRestante <= 0) {
            throw new RuntimeException(
                    "La quantité prévue pour la catégorie "
                            + linge.getCategorie()
                            + " est déjà entièrement remise."
            );
        }

        Collecte collecteImputee = choisirCollecte(remise, linge.getCategorie());

        // Mémorisés pour pouvoir annuler la remise de ce linge
        EtatLinge etatAvant = linge.getEtat();
        Long emplacementAvant = linge.getArsenalEmplacementID();

        linge.setEtat(EtatLinge.LIVRE);
        linge.setArsenalEmplacementID(null);
        lingeRepository.save(linge);

        remiseLingeLingeRepository.save(
                RemiseLingeLinge.builder()
                        .remiseLinge(remise)
                        .linge(linge)
                        .collecte(collecteImputee)
                        .categorie(linge.getCategorie())
                        .etatAvant(etatAvant)
                        .arsenalEmplacementIdAvant(emplacementAvant)
                        .build()
        );

        item.setQuantiteRemise(
                item.getQuantiteRemise() + 1
        );

        remiseLingeItemRepository.save(item);

        mettreAJourEtatRemise(remise);

        verifierEtCloturerCollecte(collecteImputee.getId());

        return item;
    }


    @Transactional
    public RemiseLingeItem annulerLinge(
            Long remiseId,
            String codeBarre
    ) {
        RemiseLinge remise = remiseLingeRepository.findById(remiseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "RemiseLinge",
                                "id",
                                remiseId
                        )
                );

        Linge linge = lingeRepository.findByCodeBarre(codeBarre)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "codeBarre",
                                codeBarre
                        )
                );

        RemiseLingeLinge lien = remiseLingeLingeRepository
                .findByRemiseLinge_IdAndLinge_Id(remiseId, linge.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Le linge " + codeBarre
                                        + " ne fait pas partie de la remise "
                                        + remise.getNumeroLivraison() + "."
                        )
                );

        if (lien.getDateRangement() != null) {
            throw new RuntimeException(
                    "Le linge " + codeBarre
                            + " est déjà rangé en arsenal : annulez d'abord son rangement."
            );
        }

        if (linge.getEtat() != EtatLinge.LIVRE) {
            throw new RuntimeException(
                    "Le linge " + codeBarre + " n'est plus à l'état LIVRE ("
                            + linge.getEtat() + ") : sa remise ne peut plus être annulée."
            );
        }

        // Linge : état et étagère d'avant la remise (PLIE par défaut pour les anciennes remises)
        linge.setEtat(lien.getEtatAvant() != null ? lien.getEtatAvant() : EtatLinge.PLIE);
        linge.setArsenalEmplacementID(lien.getArsenalEmplacementIdAvant());
        lingeRepository.save(linge);

        RemiseLingeItem item =
                remiseLingeItemRepository
                        .findByRemiseLingeAndCategorie(remise, lien.getCategorie())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Aucun item trouvé pour la catégorie : "
                                                + lien.getCategorie()
                                )
                        );

        item.setQuantiteRemise(Math.max(0, item.getQuantiteRemise() - 1));
        remiseLingeItemRepository.save(item);

        Collecte collecte = lien.getCollecte() != null ? lien.getCollecte() : remise.getCollecte();

        remiseLingeLingeRepository.delete(lien);
        remiseLingeLingeRepository.flush();

        // La collecte attend de nouveau ce linge : on la rouvre si elle était clôturée
        if (collecte != null && collecte.getEtatCollecte() == EtatCollecte.CLOTUREE) {
            collecte.setEtatCollecte(EtatCollecte.PARTIELLEMENT_LIVREE);
            collecteRepository.save(collecte);
        }

        mettreAJourEtatRemise(remise);

        return item;
    }

    private Collecte choisirCollecte(RemiseLinge remise, CategLinge categorie) {

        List<Collecte> collectes =
                remiseLingeCollecteRepository.findCollectesDeRemise(remise.getId());

        if (collectes.isEmpty()) {
            return remise.getCollecte();
        }

        for (Collecte collecte : collectes) {
            if (calculerRestes(collecte.getId()).getOrDefault(categorie, 0) > 0) {
                return collecte;
            }
        }

        throw new RuntimeException(
                "Aucune collecte de cette remise n'attend encore la catégorie " + categorie + "."
        );
    }

    @Transactional
    public StatutCollecteDTO getStatutCollecte(Long collecteId) {

        collecteRepository.findById(collecteId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collecte",
                                "id",
                                collecteId
                        )
                );

        ResumeCollecteDTO resume =
                collecteService.getResume(collecteId);

        if (resume == null) {
            throw new RuntimeException(
                    "Impossible de récupérer le résumé de la collecte."
            );
        }

        Map<CategLinge, Integer> quantitesLivrees =
                getQuantitesRemisesParCategorie(collecteId);

        List<StatutCategorieDTO> categories =
                resume.getCategories()
                        .stream()
                        .map(categorieDTO -> {

                            CategLinge categorie =
                                    categorieDTO.getCategorie();

                            int quantiteCollectee =
                                    categorieDTO.getQuantite() == null
                                            ? 0
                                            : categorieDTO.getQuantite();

                            int quantiteLivree =
                                    Math.min(
                                            quantiteCollectee,
                                            quantitesLivrees.getOrDefault(categorie, 0)
                                    );

                            int quantiteRestante =
                                    Math.max(
                                            0,
                                            quantiteCollectee - quantiteLivree
                                    );

                            return StatutCategorieDTO.builder()
                                    .categorie(categorie)
                                    .quantiteCollectee(quantiteCollectee)
                                    .quantiteLivree(quantiteLivree)
                                    .quantiteRestante(quantiteRestante)
                                    .build();
                        })
                        .toList();

        int totalCollecte =
                categories.stream()
                        .mapToInt(StatutCategorieDTO::getQuantiteCollectee)
                        .sum();

        int totalLivre =
                categories.stream()
                        .mapToInt(StatutCategorieDTO::getQuantiteLivree)
                        .sum();

        int totalRestant =
                Math.max(
                        0,
                        totalCollecte - totalLivre
                );

        int pourcentage = totalCollecte == 0
                ? 0
                : (int) Math.round(
                (totalLivre * 100.0) / totalCollecte
        );

        String statut;

        if (totalLivre == 0) {
            statut = "EN_ATTENTE";
        } else if (totalRestant == 0) {
            statut = "COMPLETEE";
        } else {
            statut = "PARTIELLE";
        }

        return StatutCollecteDTO.builder()
                .collecteId(collecteId)
                .totalCollecte(totalCollecte)
                .totalLivre(totalLivre)
                .totalRestant(totalRestant)
                .pourcentage(pourcentage)
                .statut(statut)
                .categories(categories)
                .build();
    }

    private void verifierEtCloturerCollecte(Long collecteId) {

        StatutCollecteDTO statut =
                getStatutCollecte(collecteId);

        if (statut.getTotalRestant() == 0
                && statut.getTotalCollecte() > 0) {

            Collecte collecte =
                    collecteRepository.findById(collecteId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Collecte",
                                            "id",
                                            collecteId
                                    )
                            );

            collecte.setEtatCollecte(
                    EtatCollecte.CLOTUREE
            );

            collecteRepository.save(collecte);
        }
    }
}