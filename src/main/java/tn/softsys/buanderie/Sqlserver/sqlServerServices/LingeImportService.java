package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.Sqlserver.dtos.LingeImportableProjectionDto;
import tn.softsys.buanderie.dto.Linge.LingeImportRequest;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.repository.LingeRepository;

@Service
@RequiredArgsConstructor
public class LingeImportService {

    private final CubixImportClient cubixImportClient;

    private final LingeRepository lingeRepository;

    private final CategorieLingeResolver categorieLingeResolver;


    public Page<LingeImportableProjectionDto> getLingesAImporter(
            int page,
            int size) {

        Page<LingeImportableProjectionDto> resultat =
                cubixImportClient.getLinges(page, size);

        resultat.getContent().forEach(linge ->
                linge.setCategorieSuggeree(
                        categorieLingeResolver.resoudre(
                                linge.getDesignation()
                        )
                )
        );

        return resultat;
    }


    /**
     * Import des linges sélectionnés depuis Cubix
     * vers PostgreSQL.
     *
     * Le frontend envoie uniquement les IDs
     * des linges qu'il souhaite importer.
     */
    @Transactional
    public List<Linge> importerLinges(
            List<LingeImportRequest> requests) {


        // =========================================================
        // 1. Vérification de la requête
        // =========================================================

        if (requests == null || requests.isEmpty()) {

            throw new RuntimeException(
                    "Aucun linge à importer"
            );
        }


        // =========================================================
        // 2. Récupération des IDs demandés
        // =========================================================

        List<Long> ids = requests.stream()

                .map(LingeImportRequest::getId)

                .filter(Objects::nonNull)

                .distinct()

                .toList();


        if (ids.isEmpty()) {

            throw new RuntimeException(
                    "Aucun ID de linge valide"
            );
        }


        // =========================================================
        // 3. Récupération des linges depuis Cubix
        // =========================================================
        //
        // IMPORTANT :
        // On ne fait plus :
        //
        // sqlServerRepository.findLingesImportablesByIds(ids)
        //
        // On passe maintenant par l'API ASP.NET Cubix.
        //

        List<LingeImportableProjectionDto> sources =
                cubixImportClient.getLingesByIds(ids);


        if (sources == null || sources.isEmpty()) {

            throw new RuntimeException(
                    "Aucun linge importable trouvé dans Cubix"
            );
        }


        // =========================================================
        // 4. Création d'une Map ID -> Linge Legacy
        // =========================================================

        Map<Long, LingeImportableProjectionDto> sourceMap =
                sources.stream()

                        .filter(source ->
                                source.getId() != null
                        )

                        .collect(Collectors.toMap(

                                LingeImportableProjectionDto::getId,

                                Function.identity(),

                                (a, b) -> a
                        ));


        // =========================================================
        // 5. Liste des nouveaux linges PostgreSQL
        // =========================================================

        List<Linge> lingesAImporter =
                new ArrayList<>();


        // =========================================================
        // 6. Traitement de chaque linge demandé
        // =========================================================

        for (LingeImportRequest request : requests) {


            // -----------------------------------------------------
            // Vérification ID
            // -----------------------------------------------------

            if (request.getId() == null) {

                throw new RuntimeException(
                        "L'ID du linge est obligatoire"
                );
            }


            // -----------------------------------------------------
            // Récupération du linge depuis Cubix
            // -----------------------------------------------------

            LingeImportableProjectionDto source =
                    sourceMap.get(request.getId());


            if (source == null) {

                throw new RuntimeException(
                        "Linge introuvable ou non importable : "
                                + request.getId()
                );
            }


            // -----------------------------------------------------
            // Vérification catégorie
            // -----------------------------------------------------

            CategLinge categorie =
                    request.getCategorie() != null
                            ? request.getCategorie()
                            : categorieLingeResolver.resoudre(
                            source.getDesignation()
                    );


            // -----------------------------------------------------
            // Vérification service
            // -----------------------------------------------------

            if (request.getServiceId() == null) {

                throw new RuntimeException(
                        "Le service est obligatoire pour le linge "
                                + source.getId()
                );
            }


            // =====================================================
            // 7. Vérifier si le linge Legacy est déjà importé
            // =====================================================

            Optional<Linge> lingeExistant =
                    lingeRepository
                            .findByLegacyLotArticleImmobilisationId(
                                    source.getId()
                            );


            if (lingeExistant.isPresent()) {

                throw new RuntimeException(
                        "Le linge "
                                + source.getId()
                                + " est déjà importé"
                );
            }


            // =====================================================
            // 8. Vérification du code-barres
            // =====================================================

            if (source.getCodeBarre() == null
                    || source.getCodeBarre().isBlank()) {

                throw new RuntimeException(
                        "Le linge "
                                + source.getId()
                                + " ne possède pas de code-barres"
                );
            }


            // =====================================================
            // 9. Vérifier si le code-barres existe déjà
            // =====================================================

            Optional<Linge> lingeAvecMemeCode =
                    lingeRepository.findByCodeBarre(
                            source.getCodeBarre()
                    );


            if (lingeAvecMemeCode.isPresent()) {

                Linge existant =
                        lingeAvecMemeCode.get();


                throw new RuntimeException(
                        "Le code-barres "
                                + source.getCodeBarre()
                                + " est déjà utilisé par le linge "
                                + "PostgreSQL ID="
                                + existant.getId()
                                + ", LegacyID="
                                + existant.getLegacyLotArticleImmobilisationId()
                );
            }


            // =====================================================
            // 10. Création du linge PostgreSQL
            // =====================================================

            Linge linge = Linge.builder()

                    // ID Legacy
                    .legacyLotArticleImmobilisationId(
                            source.getId()
                    )

                    // Désignation Legacy
                    .designation(
                            source.getDesignation()
                    )

                    // Code-barres Legacy
                    .codeBarre(
                            source.getCodeBarre()
                    )

                    // Etat immobilisation Legacy
                    .etatImmobilisation(
                            mapEtatImmobilisation(
                                    source.getEtatActif()
                            )
                    )

                    // Catégorie choisie dans le frontend
                    .categorie(
                            categorie
                    )

                    // Service choisi dans le frontend
                    .serviceId(
                            request.getServiceId()
                    )

                    // Etat initial du linge
                    .etat(
                            EtatLinge.STOCKE
                    )

                    // Nombre de lavages initial
                    .nbLavage(0)

                    .build();


            lingesAImporter.add(linge);
        }


        // =========================================================
        // 11. Sauvegarde PostgreSQL
        // =========================================================

        return lingeRepository.saveAll(
                lingesAImporter
        );
    }


    /**
     * Conversion de l'état Legacy SQL Server
     * vers l'enum EtatImmobilisation du nouveau système.
     */
    private EtatImmobilisation mapEtatImmobilisation(
            Integer etatActif) {


        if (etatActif == null) {
            return null;
        }


        return switch (etatActif) {

            case 0 ->
                    EtatImmobilisation.DISPONIBLE;

            case 4 ->
                    EtatImmobilisation.EN_COURS;

            case 1 ->
                    EtatImmobilisation.VENDU;

            case 2 ->
                    EtatImmobilisation.PERDU;

            case 5 ->
                    EtatImmobilisation.MISE_AU_REBUT;

            case 6 ->
                    EtatImmobilisation.VOLE;

            case 10 ->
                    EtatImmobilisation.ANNULE;

            default ->
                    throw new RuntimeException(
                            "Etat d'immobilisation Legacy inconnu : "
                                    + etatActif
                    );
        };
    }
}