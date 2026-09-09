package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.LingeImportRepository;
import tn.softsys.buanderie.Sqlserver.projection.LingeImportableProjection;
import tn.softsys.buanderie.dto.Linge.LingeImportRequest;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.repository.LingeRepository;

@Service
@RequiredArgsConstructor
public class LingeImportService {

    private final LingeImportRepository sqlServerRepository;
    private final LingeRepository lingeRepository;


    @Transactional
    public List<Linge> importerLinges(
            List<LingeImportRequest> requests) {


        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException(
                    "Aucun linge à importer"
            );
        }


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


        List<LingeImportableProjection> sources =
                sqlServerRepository.findLingesImportablesByIds(ids);

        if (sources.isEmpty()) {
            throw new RuntimeException(
                    "Aucun linge importable trouvé dans SQL Server"
            );
        }



        Map<Long, LingeImportableProjection> sourceMap =
                sources.stream()
                        .collect(Collectors.toMap(
                                LingeImportableProjection::getId,
                                Function.identity()
                        ));



        List<Linge> lingesAImporter = new ArrayList<>();


        for (LingeImportRequest request : requests) {


            if (request.getId() == null) {
                throw new RuntimeException(
                        "L'ID du linge est obligatoire"
                );
            }

            LingeImportableProjection source =
                    sourceMap.get(request.getId());

            if (source == null) {
                throw new RuntimeException(
                        "Linge introuvable ou non importable : "
                                + request.getId()
                );
            }


            CategLinge categorie =
                    request.getCategorie();

            if (categorie == null) {
                throw new RuntimeException(
                        "La catégorie est obligatoire pour le linge "
                                + source.getId()
                );
            }


            if (request.getServiceId() == null) {
                throw new RuntimeException(
                        "Le service est obligatoire pour le linge "
                                + source.getId()
                );
            }

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


            if (source.getCodeBarre() == null
                    || source.getCodeBarre().isBlank()) {

                throw new RuntimeException(
                        "Le linge "
                                + source.getId()
                                + " ne possède pas de code-barres"
                );
            }



            Optional<Linge> lingeAvecMemeCode =
                    lingeRepository.findByCodeBarre(source.getCodeBarre());

            if (lingeAvecMemeCode.isPresent()) {

                Linge existant = lingeAvecMemeCode.get();

                throw new RuntimeException(
                        "Le code-barres " + source.getCodeBarre()
                                + " est déjà utilisé par le linge PostgreSQL ID="
                                + existant.getId()
                                + ", LegacyID="
                                + existant.getLegacyLotArticleImmobilisationId()
                );
            }

            Linge linge = Linge.builder()

                    .legacyLotArticleImmobilisationId(
                            source.getId()
                    )

                    .designation(
                            source.getDesignation()
                    )


                    .codeBarre(
                            source.getCodeBarre()
                    )

                    .etatImmobilisation(
                            mapEtatImmobilisation(
                                    source.getEtatActif()
                            )
                    )


                    .categorie(
                            categorie
                    )


                    .serviceId(
                            request.getServiceId()
                    )


                    .etat(
                            EtatLinge.STOCKE
                    )

                    .nbLavage(0)

                    .build();



            lingesAImporter.add(linge);
        }

        return lingeRepository.saveAll(
                lingesAImporter
        );
    }

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
                            "Etat d'immobilisation SQL Server "
                                    + "inconnu : "
                                    + etatActif
                    );
        };
    }
}