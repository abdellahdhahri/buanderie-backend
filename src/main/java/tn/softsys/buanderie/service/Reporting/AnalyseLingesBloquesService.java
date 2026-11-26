package tn.softsys.buanderie.service.Reporting;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.SeuilEtatLinge;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.LingeBloqueRepository;
import tn.softsys.buanderie.repository.SeuilEtatLingeRepository;


@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyseLingesBloquesService {

    /** Seuils créés automatiquement au premier usage (en heures). */
    private static final Map<EtatLinge, Integer> SEUILS_PAR_DEFAUT = new EnumMap<>(EtatLinge.class);

    static {
        SEUILS_PAR_DEFAUT.put(EtatLinge.COLLECTE, 24);
        SEUILS_PAR_DEFAUT.put(EtatLinge.INFECTE_COLLECTE, 12);
        SEUILS_PAR_DEFAUT.put(EtatLinge.RECEPTION, 12);
        SEUILS_PAR_DEFAUT.put(EtatLinge.EN_TRI, 6);
        SEUILS_PAR_DEFAUT.put(EtatLinge.EN_LAVAGE, 4);
        SEUILS_PAR_DEFAUT.put(EtatLinge.LAVE, 8);
        SEUILS_PAR_DEFAUT.put(EtatLinge.EN_SECHAGE, 4);
        SEUILS_PAR_DEFAUT.put(EtatLinge.SECHE, 12);
        SEUILS_PAR_DEFAUT.put(EtatLinge.PLIE, 24);
        SEUILS_PAR_DEFAUT.put(EtatLinge.INFECTE_EN_LAVAGE, 4);
        SEUILS_PAR_DEFAUT.put(EtatLinge.INFECTE_LAVE, 12);
        SEUILS_PAR_DEFAUT.put(EtatLinge.INFECTE_ECART, 24);
        // LIVRE : uniquement les linges remis non rangés à l'arsenal (pas ceux sur un lit)
        SEUILS_PAR_DEFAUT.put(EtatLinge.LIVRE, 12);
    }

    private final LingeBloqueRepository lingeBloqueRepository;
    private final SeuilEtatLingeRepository seuilRepository;
    private final NotificationService notificationService;

    // ------------------------------------------------------------------
    // Analyse planifiée
    // ------------------------------------------------------------------

    @Scheduled(
            cron = "${buanderie.analyse.linges-bloques.cron:0 */30 * * * *}",
            zone = "${buanderie.analyse.linges-bloques.zone:Africa/Tunis}"
    )
    public void analysePlanifiee() {
        try {
            Map<String, Object> resultat = analyserEtNotifier();
            log.info("Analyse des linges bloqués : {} linge(s) concerné(s)", resultat.get("total"));
        } catch (Exception e) {
            log.error("Analyse des linges bloqués échouée : {}", e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> analyserEtNotifier() {
        List<Map<String, Object>> liste = calculerBloques(null, null);
        Map<String, Object> resume = resumer(liste);

        if (!liste.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Long> parEtat = (Map<String, Long>) resume.get("parEtat");
            List<String> exemples = liste.stream()
                    .limit(5)
                    .map(l -> (String) l.get("codeBarre"))
                    .toList();
            notificationService.notifierLingesBloques(liste.size(), parEtat, exemples);
        }

        return resume;
    }

    // ------------------------------------------------------------------
    // Rapport (tableau de bord)
    // ------------------------------------------------------------------

    @Transactional
    public Map<String, Object> getRapport(EtatLinge etat, Long serviceId, int page, int size) {
        List<Map<String, Object>> liste = calculerBloques(etat, serviceId);

        int taille = Math.max(1, Math.min(size, 500));
        int debut = Math.max(0, page) * taille;
        int fin = Math.min(debut + taille, liste.size());

        Map<String, Object> rapport = resumer(liste);
        rapport.put("page", Math.max(0, page));
        rapport.put("size", taille);
        rapport.put("totalPages", (int) Math.ceil(liste.size() / (double) taille));
        rapport.put("linges", debut < liste.size() ? liste.subList(debut, fin) : List.of());
        return rapport;
    }

    // ------------------------------------------------------------------
    // Seuils configurables
    // ------------------------------------------------------------------

    @Transactional
    public List<SeuilEtatLinge> getSeuils() {
        initialiserSeuilsManquants();
        return seuilRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getEtat().ordinal()))
                .toList();
    }

    @Transactional
    public SeuilEtatLinge modifierSeuil(EtatLinge etat, Integer dureeMaxHeures, Boolean actif) {
        initialiserSeuilsManquants();

        SeuilEtatLinge seuil = seuilRepository.findByEtat(etat)
                .orElseGet(() -> SeuilEtatLinge.builder()
                        .etat(etat)
                        .dureeMaxHeures(24)
                        .actif(true)
                        .build());

        if (dureeMaxHeures != null) {
            if (dureeMaxHeures <= 0) {
                throw new BusinessException("La durée maximale doit être supérieure à 0 heure.");
            }
            seuil.setDureeMaxHeures(dureeMaxHeures);
        }
        if (actif != null) {
            seuil.setActif(actif);
        }
        return seuilRepository.save(seuil);
    }

    // ------------------------------------------------------------------
    // Interne
    // ------------------------------------------------------------------

    private void initialiserSeuilsManquants() {
        Set<EtatLinge> existants = seuilRepository.findAll().stream()
                .map(SeuilEtatLinge::getEtat)
                .collect(Collectors.toSet());

        SEUILS_PAR_DEFAUT.forEach((etat, heures) -> {
            if (!existants.contains(etat)) {
                seuilRepository.save(SeuilEtatLinge.builder()
                        .etat(etat)
                        .dureeMaxHeures(heures)
                        .actif(true)
                        .build());
            }
        });
    }

    private Map<EtatLinge, Integer> seuilsActifs() {
        initialiserSeuilsManquants();
        Map<EtatLinge, Integer> seuils = new EnumMap<>(EtatLinge.class);
        for (SeuilEtatLinge s : seuilRepository.findAll()) {
            if (Boolean.TRUE.equals(s.getActif()) && s.getDureeMaxHeures() != null) {
                seuils.put(s.getEtat(), s.getDureeMaxHeures());
            }
        }
        return seuils;
    }

    private List<Map<String, Object>> calculerBloques(EtatLinge etatFiltre, Long serviceFiltre) {
        Map<EtatLinge, Integer> seuils = seuilsActifs();
        if (etatFiltre != null) {
            Integer seuilEtat = seuils.get(etatFiltre);
            seuils.clear();
            if (seuilEtat != null) {
                seuils.put(etatFiltre, seuilEtat);
            }
        }
        if (seuils.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDateTime maintenant = LocalDateTime.now();
        List<Map<String, Object>> bloques = new ArrayList<>();

        // Linges dans les états du circuit (hors LIVRE)
        Set<EtatLinge> etatsCircuit = seuils.keySet().stream()
                .filter(e -> e != EtatLinge.LIVRE)
                .collect(Collectors.toSet());

        List<Object[]> lignes = new ArrayList<>();
        if (!etatsCircuit.isEmpty()) {
            lignes.addAll(lingeBloqueRepository.findLingesParEtats(etatsCircuit));
        }
        // Linges remis mais non rangés : seul le rangement (STOCKE) lève le doute
        if (seuils.containsKey(EtatLinge.LIVRE)) {
            lignes.addAll(lingeBloqueRepository.findLingesLivresNonRanges());
        }

        for (Object[] row : lignes) {
            Long serviceId = (Long) row[5];
            if (serviceFiltre != null && !serviceFiltre.equals(serviceId)) {
                continue;
            }



            EtatLinge etat = (EtatLinge) row[4];
            Integer seuil = seuils.get(etat);

            LocalDateTime depuis = row[6] != null ? (LocalDateTime) row[6] : (LocalDateTime) row[7];
            if (seuil == null || depuis == null) {
                continue;
            }

            long heures = Duration.between(depuis, maintenant).toHours();
            if (heures < seuil) {
                continue;
            }

            CategLinge categorie = (CategLinge) row[3];

            Map<String, Object> linge = new LinkedHashMap<>();
            linge.put("id", row[0]);
            linge.put("codeBarre", row[1]);
            linge.put("designation", row[2]);
            linge.put("categorie", categorie != null ? categorie.name() : null);
            linge.put("etat", etat.name());
            linge.put("serviceId", serviceId);
            linge.put("depuis", depuis);
            linge.put("heuresEcoulees", heures);
            linge.put("seuilHeures", seuil);
            linge.put("retardHeures", heures - seuil);
            // Remis mais jamais rangé : peut être un linge manquant
            boolean manquantPossible = etat == EtatLinge.LIVRE;
            linge.put("typeAlerte", manquantPossible ? "MANQUANT_POSSIBLE" : "BLOQUE");
            linge.put("manquantPossible", manquantPossible);
            bloques.add(linge);
        }

        bloques.sort((a, b) -> Long.compare((Long) b.get("retardHeures"), (Long) a.get("retardHeures")));
        return bloques;
    }

    private Map<String, Object> resumer(List<Map<String, Object>> liste) {
        Map<String, Long> parEtat = new LinkedHashMap<>();
        for (Map<String, Object> l : liste) {
            parEtat.merge((String) l.get("etat"), 1L, Long::sum);
        }

        long manquantsPossibles = liste.stream()
                .filter(l -> Boolean.TRUE.equals(l.get("manquantPossible")))
                .count();

        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("dateAnalyse", LocalDateTime.now());
        resume.put("total", liste.size());
        resume.put("manquantsPossibles", manquantsPossibles);
        resume.put("parEtat", parEtat);
        return resume;
    }
}