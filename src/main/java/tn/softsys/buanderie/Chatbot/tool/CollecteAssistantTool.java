package tn.softsys.buanderie.Chatbot.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Chatbot.dto.CollecteAnalyseResponse;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.CollecteLinge;
import tn.softsys.buanderie.entity.enums.EtatCollecte;
import tn.softsys.buanderie.entity.enums.TypeCollecte;
import tn.softsys.buanderie.repository.CollecteLingeRepository;
import tn.softsys.buanderie.repository.CollecteRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CollecteAssistantTool {

    private final CollecteRepository collecteRepository;
    private final CollecteLingeRepository collecteLingeRepository;


    public CollecteAnalyseResponse analyser(
            String periode,
            List<Long> serviceIds,
            Long personnelId,
            String typeCollecte,
            String etatCollecte
    ) {

        // =========================================================
        // 1. Calcul de la période
        // =========================================================

        LocalDateTime maintenant = LocalDateTime.now();

        LocalDateTime debut;
        LocalDateTime fin = maintenant;

        switch (periode == null ? "TOUTES" : periode) {

            case "AUJOURD_HUI":

                debut = LocalDate.now()
                        .atStartOfDay();

                break;

            case "7_DERNIERS_JOURS":

                debut = maintenant.minusDays(7);

                break;

            case "30_DERNIERS_JOURS":

                debut = maintenant.minusDays(30);

                break;

            case "TOUTES":

                debut = LocalDateTime.of(
                        2000,
                        1,
                        1,
                        0,
                        0
                );

                break;

            default:

                throw new IllegalArgumentException(
                        "Période inconnue : " + periode
                );
        }


        // =========================================================
        // 2. Récupération des collectes
        // =========================================================

        List<Collecte> collectes;


        if (serviceIds != null && !serviceIds.isEmpty()) {

            collectes =
                    collecteRepository
                            .findByServiceInAndDateCollecteBetween(
                                    serviceIds,
                                    debut,
                                    fin
                            );

        } else if (personnelId != null) {

            collectes =
                    collecteRepository
                            .findByPersonnelIdAndDateCollecteBetween(
                                    personnelId,
                                    debut,
                                    fin
                            );

        } else if (typeCollecte != null) {

            TypeCollecte type =
                    TypeCollecte.valueOf(
                            typeCollecte.toUpperCase()
                    );

            collectes =
                    collecteRepository
                            .findByTypeCollecteAndDateCollecteBetween(
                                    type,
                                    debut,
                                    fin
                            );

        } else if (etatCollecte != null) {

            EtatCollecte etat =
                    EtatCollecte.valueOf(
                            etatCollecte.toUpperCase()
                    );

            collectes =
                    collecteRepository
                            .findByEtatCollecteAndDateCollecteBetween(
                                    etat,
                                    debut,
                                    fin
                            );

        } else {

            collectes =
                    collecteRepository.findByPeriode(
                            debut,
                            fin
                    );
        }


        // =========================================================
        // 3. Initialisation des statistiques
        // =========================================================

        long totalAttendus = 0;
        long totalScannes = 0;
        long totalPropres = 0;
        long totalInfectes = 0;
        long totalManquants = 0;


        Map<String, Long> parEtat =
                new LinkedHashMap<>();

        Map<String, Long> parType =
                new LinkedHashMap<>();

        Map<String, Long> parService =
                new LinkedHashMap<>();

        Map<String, Long> parCategorie =
                new LinkedHashMap<>();


        // =========================================================
        // 4. Analyse des collectes
        // =========================================================

        for (Collecte collecte : collectes) {

            // -----------------------------------------------------
            // Etat
            // -----------------------------------------------------

            if (collecte.getEtatCollecte() != null) {

                parEtat.merge(
                        collecte.getEtatCollecte().name(),
                        1L,
                        Long::sum
                );
            }


            // -----------------------------------------------------
            // Type
            // -----------------------------------------------------

            if (collecte.getTypeCollecte() != null) {

                parType.merge(
                        collecte.getTypeCollecte().name(),
                        1L,
                        Long::sum
                );
            }


            // -----------------------------------------------------
            // Service
            // -----------------------------------------------------

            if (collecte.getService() != null) {

                parService.merge(
                        String.valueOf(
                                collecte.getService()
                        ),
                        1L,
                        Long::sum
                );
            }


            // -----------------------------------------------------
            // Quantités
            // -----------------------------------------------------

            if (collecte.getNbAttendus() != null) {

                totalAttendus +=
                        collecte.getNbAttendus();
            }

            if (collecte.getNbScannésBlanc() != null) {

                totalScannes +=
                        collecte.getNbScannésBlanc();
            }

            if (collecte.getNbPropres() != null) {

                totalPropres +=
                        collecte.getNbPropres();
            }

            if (collecte.getNbInfectésPrésumés() != null) {

                totalInfectes +=
                        collecte.getNbInfectésPrésumés();
            }

            if (collecte.getNbManquants() != null) {

                totalManquants +=
                        collecte.getNbManquants();
            }


            // -----------------------------------------------------
            // Catégories réellement scannées
            // -----------------------------------------------------

            List<CollecteLinge> linges =
                    collecteLingeRepository
                            .findByCollecte_Id(
                                    collecte.getId()
                            );

            for (CollecteLinge collecteLinge : linges) {

                if (collecteLinge.getLinge() == null) {
                    continue;
                }

                if (collecteLinge.getLinge().getCategorie() == null) {
                    continue;
                }

                String categorie =
                        collecteLinge
                                .getLinge()
                                .getCategorie()
                                .name();

                parCategorie.merge(
                        categorie,
                        1L,
                        Long::sum
                );
            }
        }


        // =========================================================
        // 5. Total des pièces réellement scannées
        // =========================================================

        long totalPiecesScannees =
                parCategorie.values()
                        .stream()
                        .mapToLong(Long::longValue)
                        .sum();


        // 6. Nombre de collectes ouvertes / terminées

        long collectesOuvertes = 0;
        long collectesTerminees = 0;

        for (Collecte collecte : collectes) {

            if (collecte.getEtatCollecte() == null) {
                continue;
            }

            String etat =
                    collecte
                            .getEtatCollecte()
                            .name()
                            .toUpperCase();

            if (etat.contains("OUVERT")
                    || etat.contains("EN_COURS")
                    || etat.contains("ENCOURS")) {

                collectesOuvertes++;

            } else if (etat.contains("TERM")
                    || etat.contains("FERM")
                    || etat.contains("CLOT")) {

                collectesTerminees++;
            }
        }



        return CollecteAnalyseResponse.builder()
                .periode(
                        periode == null
                                ? "TOUTES"
                                : periode
                )
                .totalCollectes(
                        collectes.size()
                )
                .collectesOuvertes(
                        collectesOuvertes
                )
                .collectesTerminees(
                        collectesTerminees
                )
                .totalPiecesScannees(
                        totalPiecesScannees
                )
                .totalAttendus(
                        totalAttendus
                )
                .totalPropres(
                        totalPropres
                )
                .totalInfectesPresumes(
                        totalInfectes
                )
                .totalManquants(
                        totalManquants
                )
                .parEtat(parEtat)
                .parType(parType)
                .parService(parService)
                .parCategorie(parCategorie)
                .build();
    }
}