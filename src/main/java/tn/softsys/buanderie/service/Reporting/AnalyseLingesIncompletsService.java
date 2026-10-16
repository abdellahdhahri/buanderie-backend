package tn.softsys.buanderie.service.Reporting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.dto.Linge.LingeIncompletDTO;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.repository.LingeIncompletRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyseLingesIncompletsService {

    public static final String POIDS = "POIDS";
    public static final String DUREE_VIE = "DUREE_VIE";
    public static final String CATEGORIE = "CATEGORIE";
    public static final String SERVICE = "SERVICE";
    public static final String DESIGNATION = "DESIGNATION";

    private static final List<String> ATTRIBUTS = List.of(POIDS, DUREE_VIE, CATEGORIE, SERVICE, DESIGNATION);

    private static final List<EtatLinge> ETATS_EXCLUS =
            List.of(EtatLinge.HORS_SERVICE, EtatLinge.MANQUANT);

    private static final List<EtatImmobilisation> IMMOBILISATIONS_EXCLUES = List.of(
            EtatImmobilisation.VENDU,
            EtatImmobilisation.PERDU,
            EtatImmobilisation.MISE_AU_REBUT,
            EtatImmobilisation.VOLE,
            EtatImmobilisation.ANNULE
    );

    private final LingeIncompletRepository lingeIncompletRepository;
    private final NotificationService notificationService;

    @Scheduled(
           // cron = "${buanderie.analyse.linges-incomplets.cron:0 * * * * *}",
            cron = "${buanderie.analyse.linges-incomplets.cron:0 0 */4 * * *}",
            zone = "${buanderie.analyse.linges-incomplets.zone:Africa/Tunis}"
    )
    public void analysePlanifiee() {
        try {
            Map<String, Object> resultat = analyserEtNotifier();
            log.info("Analyse des linges incomplets : {} linge(s) concerné(s)", resultat.get("total"));
        } catch (Exception e) {
            log.error("Analyse des linges incomplets échouée : {}", e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> analyserEtNotifier() {
        List<LingeIncompletDTO> liste = getLingesIncomplets(null);
        Map<String, Object> resume = resumer(liste);

        if (!liste.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Long> parAttribut = (Map<String, Long>) resume.get("parAttribut");
            List<String> exemples = liste.stream()
                    .limit(10)
                    .map(LingeIncompletDTO::getCodeBarre)
                    .toList();
            notificationService.notifierLingesIncomplets(liste.size(), parAttribut, exemples);
        }

        return resume;
    }

    @Transactional(readOnly = true)
    public List<LingeIncompletDTO> getLingesIncomplets(String attribut) {
        List<LingeIncompletDTO> liste = new ArrayList<>();

        for (Object[] row : lingeIncompletRepository.findLingesIncomplets(
                ETATS_EXCLUS, IMMOBILISATIONS_EXCLUES, CategLinge.AUTRE)) {

            Long id = (Long) row[0];
            String codeBarre = (String) row[1];
            String designation = (String) row[2];
            CategLinge categorie = (CategLinge) row[3];
            EtatLinge etat = (EtatLinge) row[4];
            Long serviceId = (Long) row[5];
            Double poids = (Double) row[6];
            Integer dureVie = (Integer) row[7];

            List<String> manquants = new ArrayList<>();
            if (poids == null || poids <= 0) {
                manquants.add(POIDS);
            }
            if (dureVie == null || dureVie <= 0) {
                manquants.add(DUREE_VIE);
            }
            if (categorie == null || categorie == CategLinge.AUTRE) {
                manquants.add(CATEGORIE);
            }
            if (serviceId == null) {
                manquants.add(SERVICE);
            }
            if (designation == null || designation.isBlank()) {
                manquants.add(DESIGNATION);
            }

            if (manquants.isEmpty() || (attribut != null && !manquants.contains(attribut.toUpperCase()))) {
                continue;
            }

            liste.add(LingeIncompletDTO.builder()
                    .id(id)
                    .codeBarre(codeBarre)
                    .designation(designation)
                    .categorie(categorie != null ? categorie.name() : null)
                    .etat(etat != null ? etat.name() : null)
                    .serviceId(serviceId)
                    .poids(poids)
                    .dureVie(dureVie)
                    .attributsManquants(manquants)
                    .build());
        }

        return liste;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRapport(String attribut, int page, int size) {
        List<LingeIncompletDTO> liste = getLingesIncomplets(attribut);

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

    private Map<String, Object> resumer(List<LingeIncompletDTO> liste) {
        Map<String, Long> parAttribut = new LinkedHashMap<>();
        ATTRIBUTS.forEach(a -> parAttribut.put(a, 0L));
        liste.forEach(l -> l.getAttributsManquants().forEach(a -> parAttribut.merge(a, 1L, Long::sum)));

        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("dateAnalyse", LocalDateTime.now());
        resume.put("total", liste.size());
        resume.put("parAttribut", parAttribut);
        return resume;
    }
}
