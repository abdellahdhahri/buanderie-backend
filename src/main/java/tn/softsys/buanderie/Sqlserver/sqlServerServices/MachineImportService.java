package tn.softsys.buanderie.Sqlserver.sqlServerServices;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.AutoclaveLegacyRepository;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.AutoclaveRepository;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.LaveurImportRepository;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.LotArticleImmobilisationRepository;
import tn.softsys.buanderie.Sqlserver.dtos.AutoclaveLegacyDTO;
import tn.softsys.buanderie.Sqlserver.projection.LaveurImportableProjection;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Autoclave;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.LotArticleImmobilisation;
import tn.softsys.buanderie.dto.MachineImportRequest;
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.entity.enums.CategMachine;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.mapper.MachineLegacyMapper;
import tn.softsys.buanderie.repository.MachineRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineImportService {

    private final AutoclaveRepository autoclaveRepository;
    private final LaveurImportRepository repository;
    private final MachineRepository machineRepository;


    private final LotArticleImmobilisationRepository
            lotArticleImmobilisationRepository;


    private final MachineLegacyMapper mapper;


    @Transactional
    public int importerLaveurs() {

        List<Autoclave> laveurs =
                autoclaveRepository.findByCategorie(2);

        int compteur = 0;

        for (Autoclave source : laveurs) {

            if (machineRepository.existsById(source.getId())) {
                continue;
            }

            LotArticleImmobilisation immobilisation = null;

            Long lotId =
                    source.getLotArticleImmobilisationId();

            if (lotId != null) {

                immobilisation =
                        lotArticleImmobilisationRepository
                                .findById(lotId)
                                .orElse(null);
            }

            Machine machine =
                    mapper.convertir(
                            source,
                            immobilisation
                    );

            machineRepository.save(machine);

            compteur++;
        }

        return compteur;
    }
//    public List<LaveurImportableProjection> getLaveursImportables() {
//        return repository.findLaveursImportables();
//    }
public List<Object[]> machinesAimporter() {
    return repository.MachinesAimporter();
}





    @Transactional
    public List<Machine> importerMachines(List<MachineImportRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException("Aucune machine à importer");
        }

        //  Récupérer les IDs sélectionnés
        List<Long> ids = requests.stream()
                .map(MachineImportRequest::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            throw new RuntimeException("Aucun ID valide fourni");
        }

        // Récupérer les données depuis SQL Server
        List<LaveurImportableProjection> sources =
                repository.findMachinesBuanderieByIds(ids);

        //  Transformer la liste en Map pour retrouver rapidement
        //    les données correspondant à chaque ID
        Map<Long, LaveurImportableProjection> sourceMap =
                sources.stream()
                        .collect(Collectors.toMap(
                                LaveurImportableProjection::getId,
                                Function.identity()
                        ));

        List<Machine> machinesAImporter = new ArrayList<>();

        // Parcourir les machines demandées
        for (MachineImportRequest request : requests) {

            if (request.getId() == null) {
                throw new RuntimeException("ID machine obligatoire");
            }

            if (request.getType() == null) {
                throw new RuntimeException(
                        "Le type est obligatoire pour la machine " + request.getId()
                );
            }

            // Vérifier que l'élément existe bien dans SQL Server
            LaveurImportableProjection source =
                    sourceMap.get(request.getId());

            if (source == null) {
                throw new RuntimeException(
                        "Article introuvable ou non importable : "
                                + request.getId()
                );
            }

            //  Vérifier si cette machine est déjà importée
            Optional<Machine> machineExistante =
                    machineRepository.findByLegacyLotArticleImmobilisationId(
                            source.getId()
                    );

            if (machineExistante.isPresent()) {
                throw new RuntimeException(
                        "La machine " + source.getId()
                                + " est déjà importée"
                );
            }

            //  Création de la machine PostgreSQL
            Machine machine = Machine.builder()
                    .legacyLotArticleImmobilisationId(source.getId())
                    .code(source.getCodeBarre())
                    .designation(source.getDesignation())
                    .reference(source.getNumeroSerie())
                    .type(request.getType())
                    .categorie(CategMachine.LINGES_NORMAL)
                    .etatImmobilisation(
                            mapEtatImmobilisation(source.getEtatActif())
                    )
                    .build();

            machinesAImporter.add(machine);
        }

        //  Sauvegarde dans PostgreSQL
        return machineRepository.saveAll(machinesAImporter);
    }

    /**
     * Conversion de l'état SQL Server
     * vers l'enum PostgreSQL.
     */
    private EtatImmobilisation mapEtatImmobilisation(Integer etatActif) {

        if (etatActif == null) {
            return null;
        }

        return switch (etatActif) {

            case 0 -> EtatImmobilisation.DISPONIBLE;

            case 4 -> EtatImmobilisation.EN_COURS;

            case 1 -> EtatImmobilisation.VENDU;

            case 2 -> EtatImmobilisation.PERDU;

            case 5 -> EtatImmobilisation.MISE_AU_REBUT;

            case 6 -> EtatImmobilisation.VOLE;

            case 10 -> EtatImmobilisation.ANNULE;

            default -> throw new RuntimeException(
                    "Etat d'immobilisation SQL Server inconnu : "
                            + etatActif
            );
        };
    }
}