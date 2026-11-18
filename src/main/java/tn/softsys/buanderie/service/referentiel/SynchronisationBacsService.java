package tn.softsys.buanderie.service.referentiel;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.Bac;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeBac;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.entity.enums.UsageBac;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.BacRepository;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;

/**
 * Lecture des bacs CUBIX (Linge_Bac). Un nouveau bac devient un bac de tri blanc, vide. L'état, le poids, la catégorie,
 * le type et l'usage restent gérés par notre application : la synchronisation ne les modifie jamais.
 */
@Slf4j
@Service
public class SynchronisationBacsService {

    private static final int TAILLE_PAGE = 500;

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final BacRepository bacRepository;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationBacsService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            BacRepository bacRepository,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.bacRepository = bacRepository;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.BAC)) {
            throw new BusinessException("Les bacs ne sont pas importés : mode interne ou URL inactive.");
        }
        return recuperer(1).size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.BAC)) {
                throw new BusinessException("Les bacs sont gérés en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.BAC)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                List<DonneeBac> recus = dedoublonner(recuperer(0));
                if (recus.isEmpty()) {
                    throw new BusinessException("La source n'a renvoyé aucun bac exploitable : synchronisation annulée.");
                }

                int[] c = transactionTemplate.execute(status -> appliquer(recus));

                journal.setNbRecus(recus.size());
                journal.setNbCrees(c[0]);
                journal.setNbMisAJour(c[1]);
                journal.setNbDesactives(0);
                journal.setStatut("SUCCES");

                StringBuilder message = new StringBuilder()
                        .append(recus.size()).append(" reçus, ")
                        .append(c[0]).append(" créés, ")
                        .append(c[1]).append(" mis à jour");
                if (c[2] > 0) {
                    message.append(", ").append(c[2]).append(" ignorés (code-barres déjà utilisé)");
                }
                if (c[3] > 0) {
                    message.append(", ").append(c[3]).append(" absents de la source (conservés)");
                }
                message.append(".");
                journal.setMessage(message.toString());
            } catch (Exception e) {
                log.error("Synchronisation des bacs échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.BAC, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeBac> recuperer(int pagesMax) {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.BAC);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des bacs n'est pas configurée.");
        }

        ConfigurationIntegration config = integrationService.getConfiguration();
        String url = integrationService.urlComplete(Referentiel.BAC);

        List<JsonNode> elements = config.getTypeConnecteur() == TypeConnecteur.CUBIX
                ? connecteur.recupererListePaginee(url, config, endpoint.getCheminListe(), TAILLE_PAGE, pagesMax)
                : connecteur.recupererListe(url, config, endpoint.getCheminListe());

        return elements.stream()
                .map(e -> {
                    DonneeBac d = new DonneeBac();
                    d.idExterne = connecteur.texte(e, champs.get("idExterne"));
                    d.code = limiter(connecteur.texte(e, champs.get("code")), 100);
                    d.designation = limiter(connecteur.texte(e, champs.get("designation")), 255);
                    return d;
                })
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private List<DonneeBac> dedoublonner(List<DonneeBac> recus) {
        Map<String, DonneeBac> uniques = new LinkedHashMap<>();
        for (DonneeBac d : recus) {
            uniques.putIfAbsent(d.idExterne, d);
        }
        return List.copyOf(uniques.values());
    }

    private int[] appliquer(List<DonneeBac> recus) {
        Map<String, Bac> existants = bacRepository.findBySourceDonnee(SourceDonnee.EXTERNE).stream()
                .filter(b -> b.getIdExterne() != null)
                .collect(Collectors.toMap(Bac::getIdExterne, Function.identity(), (a, b) -> a));

        Map<String, Long> codesPris = new HashMap<>();
        for (Bac b : bacRepository.findAll()) {
            if (b.getCode() != null) {
                codesPris.putIfAbsent(b.getCode(), b.getId());
            }
        }

        int crees = 0;
        int misAJour = 0;
        int ignores = 0;
        LocalDateTime maintenant = LocalDateTime.now();

        for (DonneeBac d : recus) {
            Bac bac = existants.get(d.idExterne);

            if (bac == null) {
                if (d.code != null && codesPris.containsKey(d.code)) {
                    ignores++;
                    continue;
                }
                Bac sauve = bacRepository.save(Bac.builder()
                        .sourceDonnee(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .reference(referenceLibre(d.idExterne))
                        .code(d.code)
                        .designation(d.designation)
                        .typebac(TypeBac.BLANC)
                        .usage(UsageBac.TRI)
                        .etat(EtatBac.VIDE)
                        .dateSynchro(maintenant)
                        .build());
                if (d.code != null) {
                    codesPris.put(d.code, sauve.getId());
                }
                crees++;
                continue;
            }

            boolean modifie = false;
            if (d.designation != null && !Objects.equals(bac.getDesignation(), d.designation)) {
                bac.setDesignation(d.designation);
                modifie = true;
            }
            if (d.code != null && !Objects.equals(bac.getCode(), d.code)) {
                Long proprietaire = codesPris.get(d.code);
                if (proprietaire != null && !proprietaire.equals(bac.getId())) {
                    ignores++;
                } else {
                    bac.setCode(d.code);
                    codesPris.put(d.code, bac.getId());
                    modifie = true;
                }
            }
            if (modifie) {
                bac.setDateSynchro(maintenant);
                bacRepository.save(bac);
                misAJour++;
            }
        }

        Set<String> idsRecus = recus.stream().map(d -> d.idExterne).collect(Collectors.toSet());
        int absents = (int) existants.keySet().stream().filter(id -> !idsRecus.contains(id)).count();

        return new int[]{crees, misAJour, ignores, absents};
    }

    /** Référence unique (20 caractères maximum) pour un bac importé. */
    private String referenceLibre(String idExterne) {
        String base = "CBX-" + idExterne;
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        if (!bacRepository.existsByReference(base)) {
            return base;
        }
        return "CBX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
    }

    private String limiter(String valeur, int max) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String propre = valeur.trim();
        return propre.length() > max ? propre.substring(0, max) : propre;
    }

    private String tronquer(String message) {
        if (message == null) {
            return "Erreur inconnue";
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    private static class DonneeBac {
        private String idExterne;
        private String code;
        private String designation;
    }
}
