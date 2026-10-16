package tn.softsys.buanderie.service.management;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tn.softsys.buanderie.Sqlserver.Cubix.CubixPersonnelService;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerPersonnelRepository;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerServiceRepository;
import tn.softsys.buanderie.Sqlserver.dtos.PersonnelDto;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;
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
import tn.softsys.buanderie.repository.AgentRepository;
import tn.softsys.buanderie.repository.CollecteRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.RemiseLingeItemRepository;
import tn.softsys.buanderie.repository.RemiseLingeLingeRepository;
import tn.softsys.buanderie.repository.RemiseLingeRepository;
import tn.softsys.buanderie.repository.ServiceRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RemiseLingeService {

    private final RemiseLingeRepository remiseLingeRepository;
    private final RemiseLingeItemRepository remiseLingeItemRepository;
    private final RemiseLingeLingeRepository remiseLingeLingeRepository;

    private final CollecteRepository collecteRepository;
    private final LingeRepository lingeRepository;
    private final SqlServerServiceRepository serviceRepository;

    private final CollecteResumeService collecteService;
    private final CubixPersonnelService personnelService;



    @Transactional
    public RemiseLinge creerRemise(
            String matriculeAgentRemise,
            Long  id
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
        Collecte collecte =
                collecteRepository
                        .findFirstByPersonnelIdAndEtatCollecteNotOrderByDateCollecteDesc(
                                id,
                                EtatCollecte.CLOTUREE
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Collecte non clôturée",
                                        "matricule agent",
                                        id
                                )
                        );
        if (collecte.getService() == null) {

            throw new RuntimeException(
                    "La collecte ne possède aucun service."
            );
        }



        Optional<
                tn.softsys.buanderie.Sqlserver.sqlServerEntities.Service
                > service =
                serviceRepository.findById(
                        collecte.getService()
                );

        if (service.isEmpty()) {

            throw new ResourceNotFoundException(
                    "Service",
                    "id",
                    collecte.getService()
            );
        }



        ResumeCollecteDTO resume =
                collecteService.getResume(
                        collecte.getId()
                );

        if (resume == null ||
                resume.getCategories() == null ||
                resume.getCategories().isEmpty()) {

            throw new RuntimeException(
                    "Cette collecte ne contient aucun linge à remettre."
            );
        }


        RemiseLinge remise =
                RemiseLinge.builder()
                        .collecte(collecte)
                        .service(service.get().getId())
                        .personnelId(personnelRemise.getId())
                        .dateLivraison(LocalDateTime.now())
                        .etat(EtatLivraison.EN_COURS)
                        .build();

        remise =
                remiseLingeRepository.save(remise);


        for (ResumeCategorieDTO categorieDTO :
                resume.getCategories()) {

            CategLinge categorie =
                    categorieDTO.getCategorie();

            Integer quantiteCollectee =
                    categorieDTO.getQuantite();

            if (quantiteCollectee == null ||
                    quantiteCollectee <= 0) {

                continue;
            }



            Integer dejaRemise =
                    getQuantiteDejaRemise(
                            collecte.getId(),
                            categorie
                    );

            int resteAremettre =
                    Math.max(
                            0,
                            quantiteCollectee - dejaRemise
                    );

            int stockDisponible =
                    getStockDisponible(
                            service.get().getId(),
                            categorie
                    );


            if (resteAremettre <= 0) {
                continue;
            }


            RemiseLingeItem item =
                    RemiseLingeItem.builder()
                            .remiseLinge(remise)
                            .categorie(categorie)
                            .quantiteCollectee(
                                    quantiteCollectee
                            )
                            .quantiteDisponible(
                                    stockDisponible
                            )
                            .quantiteRemise(0)
                            .build();

            remiseLingeItemRepository.save(item);
        }

        mettreAJourEtatRemise(remise);


        return remise;
    }

    // QUANTITE DEJA REMISE POUR UNE COLLECTE

    private Integer getQuantiteDejaRemise(
            Long collecteId,
            CategLinge categorie
    ) {

        Integer quantite =
                remiseLingeItemRepository
                        .sumQuantiteRemiseByCollecteAndCategorie(
                                collecteId,
                                categorie
                        );
        return quantite == null
                ? 0
                : quantite;

    }


    // STOCK DISPONIBLE

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


    // METTRE A JOUR L'ETAT DE LA REMISE

    private void mettreAJourEtatRemise(
            RemiseLinge remise
    ) {

        List<RemiseLingeItem> items =
                remiseLingeItemRepository
                        .findByRemiseLinge(remise);

        if (items == null || items.isEmpty()) {

            remise.setEtat(
                    EtatLivraison.EN_COURS
            );

            remiseLingeRepository.save(remise);

            return;
        }


        boolean complet = true;
        boolean partiel = false;


        for (RemiseLingeItem item : items) {

            int collectee =
                    item.getQuantiteCollectee() == null
                            ? 0
                            : item.getQuantiteCollectee();

            int dejaRemisAvant =
                    getQuantiteDejaRemiseAvantRemise(
                            remise.getId(),
                            remise.getCollecte().getId(),
                            item.getCategorie()
                    );

            int totalRemis =
                    dejaRemisAvant
                            + item.getQuantiteRemise();


            if (totalRemis < collectee) {
                complet = false;
            }

            if (item.getQuantiteRemise() > 0) {
                partiel = true;
            }
        }


        if (complet) {

            remise.setEtat(
                    EtatLivraison.COMPLETEE
            );

        } else if (partiel) {

            remise.setEtat(
                    EtatLivraison.PARTIELLE
            );

        } else {

            remise.setEtat(
                    EtatLivraison.EN_ATTENTE
            );
        }


        remiseLingeRepository.save(remise);
    }


    // QUANTITE REMISE AVANT CETTE REMISE

    private Integer getQuantiteDejaRemiseAvantRemise(
            Long remiseId,
            Long collecteId,
            CategLinge categorie
    ) {

        Integer quantite =
                remiseLingeItemRepository
                        .sumQuantiteRemiseByCollecteAndCategorieBeforeRemise(
                                collecteId,
                                categorie,
                                remiseId
                        );

        return quantite == null
                ? 0
                : quantite;
    }


    // CALCUL DU RESTE POUR UNE COLLECTE

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


    // RECUPERER UNE REMISE

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


    // RECUPERER LES REMISES D'UNE COLLECTE

    public List<RemiseLinge> getByCollecte(
            Long collecteId
    ) {

        return remiseLingeRepository
                .findByCollecte_IdOrderByDateLivraisonAsc(
                        collecteId
                );
    }


    // TERMINER UNE REMISE

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

        //  Récupérer la remise
        RemiseLinge remise = remiseLingeRepository.findById(remiseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "RemiseLinge",
                                "id",
                                remiseId
                        )
                );
        // Rechercher le linge avec son code-barres
        Linge linge = lingeRepository.findByCodeBarre(codeBarre)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "codeBarre",
                                codeBarre
                        )
                );

        // Vérifier l'état du linge
        if (linge.getEtat() == EtatLinge.LIVRE) {
            throw new RuntimeException(
                    "Ce linge est déjà livré : " + codeBarre
            );
        }

        //  Vérifier le service
        if (!Objects.equals(
                linge.getServiceId(),
                remise.getService()
        )) {
            throw new RuntimeException(
                    "Ce linge n'appartient pas au service de cette remise."
            );
        }

        //  Rechercher l'item correspondant à la catégorie
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

        // Vérifier qu'il reste une quantité à remettre
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

        //  Passer le linge à LIVRE
        linge.setEtat(EtatLinge.LIVRE);
        linge.setArsenalEmplacementID(null);

        lingeRepository.save(linge);

        remiseLingeLingeRepository.save(
                RemiseLingeLinge.builder()
                        .remiseLinge(remise)
                        .linge(linge)
                        .categorie(linge.getCategorie())
                        .build()
        );

        // Incrémenter la quantité remise
        item.setQuantiteRemise(
                item.getQuantiteRemise() + 1
        );

        remiseLingeItemRepository.save(item);

        //  Recalculer l'état de la remise
        mettreAJourEtatRemise(remise);

        verifierEtCloturerCollecte(
                remise.getCollecte().getId()
        );

        return item;
    }

    @Transactional
    public StatutCollecteDTO getStatutCollecte(Long collecteId) {

        //  Vérifier que la collecte existe
        Collecte collecte = collecteRepository.findById(collecteId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collecte",
                                "id",
                                collecteId
                        )
                );

        //  Récupérer le résumé initial de la collecte
        ResumeCollecteDTO resume =
                collecteService.getResume(collecteId);

        if (resume == null) {
            throw new RuntimeException(
                    "Impossible de récupérer le résumé de la collecte."
            );
        }

        // Récupérer toutes les lignes de toutes les remises
        List<RemiseLingeItem> items =
                remiseLingeItemRepository
                        .findByRemiseLinge_Collecte_Id(collecteId);

        //  Calculer les quantités livrées par catégorie
        Map<CategLinge, Integer> quantitesLivrees =
                new java.util.HashMap<>();

        for (RemiseLingeItem item : items) {

            int quantite =
                    item.getQuantiteRemise() == null
                            ? 0
                            : item.getQuantiteRemise();

            quantitesLivrees.merge(
                    item.getCategorie(),
                    quantite,
                    Integer::sum
            );
        }

        //  Construire le statut par catégorie
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
                                    quantitesLivrees.getOrDefault(
                                            categorie,
                                            0
                                    );

                            int quantiteRestante =
                                    Math.max(
                                            0,
                                            quantiteCollectee - quantiteLivree
                                    );

                            return StatutCategorieDTO.builder()
                                    .categorie(categorie)
                                    .quantiteCollectee(
                                            quantiteCollectee
                                    )
                                    .quantiteLivree(
                                            quantiteLivree
                                    )
                                    .quantiteRestante(
                                            quantiteRestante
                                    )
                                    .build();
                        })
                        .toList();

        //  Total collecté
        int totalCollecte =
                categories.stream()
                        .mapToInt(
                                StatutCategorieDTO::getQuantiteCollectee
                        )
                        .sum();

        //  Total livré
        int totalLivre =
                categories.stream()
                        .mapToInt(
                                StatutCategorieDTO::getQuantiteLivree
                        )
                        .sum();

        //  Total restant
        int totalRestant =
                Math.max(
                        0,
                        totalCollecte - totalLivre
                );

        //  Pourcentage
        int pourcentage = totalCollecte == 0
                ? 0
                : (int) Math.round(
                (totalLivre * 100.0) / totalCollecte
        );

        // 1 Statut global
        String statut;

        if (totalLivre == 0) {

            statut = "EN_ATTENTE";

        } else if (totalRestant == 0) {

            statut = "COMPLETEE";

        } else {

            statut = "PARTIELLE";
        }

        //  Retour
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