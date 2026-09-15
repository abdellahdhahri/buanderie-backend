package tn.softsys.buanderie.Chatbot.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Chatbot.dto.LingeAnalyseResponse;
import tn.softsys.buanderie.Chatbot.dto.LingeBloqueDTO;

import tn.softsys.buanderie.Chatbot.dto.LingeEtapeTrackingDTO;
import tn.softsys.buanderie.Chatbot.dto.LingeTrackingResponse;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.repository.HistoriqueLingeRepository;
import tn.softsys.buanderie.repository.LingeRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LingeAssistantTool {

    private final LingeRepository lingeRepository;

    private final HistoriqueLingeRepository historiqueLingeRepository;


    /**
     * Analyse générale du linge.
     */
    public LingeAnalyseResponse analyser(
            List<String> etats,
            List<String> categories,
            List<Long> serviceIds,
            Boolean bloque,
            Long seuilBlocageHeures
    ) {

        // =========================================================
        // 1. Récupérer les linges
        // =========================================================

        List<Linge> linges = lingeRepository.findAll();


        // =========================================================
        // 2. Filtre état
        // =========================================================

        if (etats != null && !etats.isEmpty()) {

            Set<EtatLinge> etatsFiltres =
                    etats.stream()
                            .filter(Objects::nonNull)
                            .map(String::toUpperCase)
                            .map(EtatLinge::valueOf)
                            .collect(Collectors.toSet());

            linges = linges.stream()
                    .filter(l ->
                            l.getEtat() != null
                                    && etatsFiltres.contains(l.getEtat())
                    )
                    .toList();
        }


        // =========================================================
        // 3. Filtre catégorie
        // =========================================================

        if (categories != null && !categories.isEmpty()) {

            Set<CategLinge> categoriesFiltrees =
                    categories.stream()
                            .filter(Objects::nonNull)
                            .map(String::toUpperCase)
                            .map(CategLinge::valueOf)
                            .collect(Collectors.toSet());

            linges = linges.stream()
                    .filter(l ->
                            l.getCategorie() != null
                                    && categoriesFiltrees.contains(
                                    l.getCategorie()
                            )
                    )
                    .toList();
        }


        // =========================================================
        // 4. Filtre service
        // =========================================================

        if (serviceIds != null && !serviceIds.isEmpty()) {

            Set<Long> services =
                    new HashSet<>(serviceIds);

            linges = linges.stream()
                    .filter(l ->
                            l.getServiceId() != null
                                    && services.contains(
                                    l.getServiceId()
                            )
                    )
                    .toList();
        }


        // =========================================================
        // 5. Statistiques
        // =========================================================

        Map<String, Long> parEtat =
                new LinkedHashMap<>();

        Map<String, Long> parCategorie =
                new LinkedHashMap<>();

        Map<String, Long> parService =
                new LinkedHashMap<>();


        for (Linge linge : linges) {

            if (linge.getEtat() != null) {

                parEtat.merge(
                        linge.getEtat().name(),
                        1L,
                        Long::sum
                );
            }


            if (linge.getCategorie() != null) {

                parCategorie.merge(
                        linge.getCategorie().name(),
                        1L,
                        Long::sum
                );
            }


            if (linge.getServiceId() != null) {

                parService.merge(
                        String.valueOf(
                                linge.getServiceId()
                        ),
                        1L,
                        Long::sum
                );
            }
        }


        // =========================================================
        // 6. Détection des linges bloqués
        // =========================================================

        List<LingeBloqueDTO> lingesBloques =
                new ArrayList<>();


        if (Boolean.TRUE.equals(bloque)) {

            LocalDateTime maintenant =
                    LocalDateTime.now();


            List<Long> lingeIds =
                    linges.stream()
                            .map(Linge::getId)
                            .filter(Objects::nonNull)
                            .toList();


            if (!lingeIds.isEmpty()) {

                List<HistoriqueLinge> historiques =
                        historiqueLingeRepository
                                .findHistoriqueByLingeIds(
                                        lingeIds
                                );


                // -------------------------------------------------
                // Dernier historique par linge
                // -------------------------------------------------

                Map<Long, HistoriqueLinge> dernierHistorique =
                        new HashMap<>();


                for (HistoriqueLinge historique : historiques) {

                    if (historique.getLinge() == null) {
                        continue;
                    }

                    Long lingeId =
                            historique
                                    .getLinge()
                                    .getId();


                    dernierHistorique.putIfAbsent(
                            lingeId,
                            historique
                    );
                }


                // -------------------------------------------------
                // Vérification blocage
                // -------------------------------------------------

                for (Linge linge : linges) {

                    HistoriqueLinge historique =
                            dernierHistorique.get(
                                    linge.getId()
                            );


                    if (historique == null) {
                        continue;
                    }


                    if (historique.getDateOperation() == null) {
                        continue;
                    }


                    if (historique.getEtatApres() == null) {
                        continue;
                    }


                    long seuil =
                            getSeuilBlocageHeures(
                                    historique.getEtatApres()
                            );


                    // Si le seuil est fourni explicitement
                    if (seuilBlocageHeures != null
                            && seuilBlocageHeures > 0) {

                        seuil =
                                seuilBlocageHeures;
                    }


                    if (seuil <= 0) {
                        continue;
                    }


                    long duree =
                            Duration.between(
                                    historique.getDateOperation(),
                                    maintenant
                            ).toHours();


                    if (duree >= seuil) {

                        lingesBloques.add(
                                LingeBloqueDTO.builder()
                                        .lingeId(
                                                linge.getId()
                                        )
                                        .codeBarre(
                                                linge.getCodeBarre()
                                        )
                                        .categorie(
                                                linge.getCategorie() != null
                                                        ? linge.getCategorie().name()
                                                        : null
                                        )
                                        .etat(
                                                historique
                                                        .getEtatApres()
                                                        .name()
                                        )
                                        .serviceId(
                                                linge.getServiceId()
                                        )
                                        .dernierChangementEtat(
                                                historique
                                                        .getDateOperation()
                                        )
                                        .dureeHeures(
                                                duree
                                        )
                                        .seuilBlocageHeures(
                                                seuil
                                        )
                                        .build()
                        );
                    }
                }
            }
        }


        // =========================================================
        // 7. Bloqués par état
        // =========================================================

        Map<String, Long> bloquesParEtat =
                lingesBloques.stream()
                        .filter(
                                l -> l.getEtat() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        LingeBloqueDTO::getEtat,
                                        LinkedHashMap::new,
                                        Collectors.counting()
                                )
                        );


        // =========================================================
        // 8. Résultat
        // =========================================================

        return LingeAnalyseResponse.builder()

                .totalLinges(
                        linges.size()
                )

                .parEtat(
                        parEtat
                )

                .parCategorie(
                        parCategorie
                )

                .parService(
                        parService
                )

                .totalBloques(
                        lingesBloques.size()
                )

                .bloquesParEtat(
                        bloquesParEtat
                )

                .lingesBloques(
                        lingesBloques
                )

                .build();
    }


    /**
     * Détermine le seuil métier de blocage
     * selon l'état du linge.
     */
    private long getSeuilBlocageHeures(
            EtatLinge etat
    ) {

        return switch (etat) {

            case COLLECTE -> 12;

            case RECEPTION -> 12;

            case EN_TRI -> 12;

            case EN_LAVAGE -> 24;

            case INFECTE_COLLECTE -> 12;

            case INFECTE_EN_LAVAGE -> 24;

            case LAVE -> 24;

            case INFECTE_LAVE -> 24;

            case EN_SECHAGE -> 12;

            case SECHE -> 24;

            case PLIE -> 24;

            case COLLE -> 24;

            case PROPRE -> 24;

            default -> 0;
        };
    }


    /**
     * Tracking d'un linge particulier.
     */
    public LingeTrackingResponse tracking(
            String codeBarre
    ) {

        Linge linge =
                lingeRepository
                        .findByCodeBarre(codeBarre)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Linge introuvable : "
                                                + codeBarre
                                )
                        );


        List<HistoriqueLinge> historiques =
                historiqueLingeRepository
                        .findTrackingLinge(
                                linge.getId()
                        );


        List<LingeEtapeTrackingDTO> parcours =
                historiques.stream()
                        .map(h ->
                                LingeEtapeTrackingDTO
                                        .builder()
                                        .etat(
                                                h.getEtatApres() != null
                                                        ? h.getEtatApres().name()
                                                        : null
                                        )
                                        .date(
                                                h.getDateOperation()
                                        )
                                        .personnelId(
                                                h.getPersonnelId()
                                        )
                                        .operation(
                                                h.getTypeOperation() != null
                                                        ? h.getTypeOperation().name()
                                                        : null
                                        )
                                        .observations(
                                                h.getObservations()
                                        )
                                        .build()
                        )
                        .toList();


        return LingeTrackingResponse.builder()

                .lingeId(
                        linge.getId()
                )

                .codeBarre(
                        linge.getCodeBarre()
                )

                .categorie(
                        linge.getCategorie() != null
                                ? linge.getCategorie().name()
                                : null
                )

                .etatActuel(
                        linge.getEtat() != null
                                ? linge.getEtat().name()
                                : null
                )

                .parcours(
                        parcours
                )

                .build();
    }
}