package tn.softsys.buanderie.service.referentiel;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.Emplacement;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ArsenalRepository;
import tn.softsys.buanderie.repository.EmplacementRepository;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;

@Slf4j
@Service
public class SynchronisationEmplacementsService {

    private static final int TAILLE_PAGE = 200;

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final EmplacementRepository emplacementRepository;
    private final ArsenalRepository arsenalRepository;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationEmplacementsService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            EmplacementRepository emplacementRepository,
            ArsenalRepository arsenalRepository,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.emplacementRepository = emplacementRepository;
        this.arsenalRepository = arsenalRepository;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.EMPLACEMENT)) {
            throw new BusinessException("Les emplacements ne sont pas importés : mode interne ou URL inactive.");
        }
        return recuperer(1).size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.EMPLACEMENT)) {
                throw new BusinessException("Les emplacements sont gérés en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.EMPLACEMENT)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                List<DonneeEmplacement> recus = dedoublonner(recuperer(0));

                if (recus.isEmpty()) {
                    throw new BusinessException(
                            "La source n'a renvoyé aucun emplacement exploitable : synchronisation annulée.");
                }

                int[] c = transactionTemplate.execute(status -> appliquer(recus));

                journal.setNbRecus(recus.size());
                journal.setNbCrees(c[0]);
                journal.setNbMisAJour(c[1]);
                journal.setNbDesactives(c[2]);
                journal.setStatut("SUCCES");

                StringBuilder message = new StringBuilder()
                        .append(recus.size()).append(" reçus, ")
                        .append(c[0]).append(" créés, ")
                        .append(c[1]).append(" mis à jour, ")
                        .append(c[2]).append(" désactivés");
                if (c[3] > 0) {
                    message.append(", ").append(c[3])
                            .append(" ignorés (arsenal introuvable : synchronisez d'abord les arsenaux)");
                }
                if (c[4] > 0) {
                    message.append(", ").append(c[4]).append(" ignorés (code-barres déjà utilisé)");
                }
                message.append(".");
                journal.setMessage(message.toString());
            } catch (Exception e) {
                log.error("Synchronisation des emplacements échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.EMPLACEMENT, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeEmplacement> recuperer(int pagesMax) {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.EMPLACEMENT);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des emplacements n'est pas configurée.");
        }
        if (champs.get("arsenalId") == null) {
            throw new BusinessException("La correspondance du champ « arsenalId » des emplacements n'est pas configurée.");
        }

        ConfigurationIntegration config = integrationService.getConfiguration();
        String url = integrationService.urlComplete(Referentiel.EMPLACEMENT);

        List<JsonNode> elements = config.getTypeConnecteur() == TypeConnecteur.CUBIX
                ? connecteur.recupererListePaginee(url, config, endpoint.getCheminListe(), TAILLE_PAGE, pagesMax)
                : connecteur.recupererListe(url, config, endpoint.getCheminListe());

        if (urlSansParametres(url).toLowerCase().endsWith("/buanderieapi/arsenaux")) {
            elements = aplatirEmplacements(elements);
        }

        return elements.stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private String urlSansParametres(String url) {
        int fin = url.indexOf('?');
        return fin >= 0 ? url.substring(0, fin) : url;
    }

    /** API CUBIX Arsenaux : les emplacements sont imbriqués ; on en fait une liste à plat avec l'id de l'arsenal parent. */
    private List<JsonNode> aplatirEmplacements(List<JsonNode> arsenaux) {
        List<JsonNode> resultat = new java.util.ArrayList<>();
        for (JsonNode arsenal : arsenaux) {
            JsonNode liste = arsenal.get("emplacements");
            if (liste == null || !liste.isArray()) {
                continue;
            }
            for (JsonNode emplacement : liste) {
                com.fasterxml.jackson.databind.node.ObjectNode plat =
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
                if (emplacement.isObject()) {
                    plat.setAll((com.fasterxml.jackson.databind.node.ObjectNode) emplacement);
                }
                plat.set("arsenalId", arsenal.get("id"));
                resultat.add(plat);
            }
        }
        return resultat;
    }

    private DonneeEmplacement convertir(JsonNode element, Map<String, String> champs) {
        DonneeEmplacement d = new DonneeEmplacement();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.arsenalIdExterne = connecteur.texte(element, champs.get("arsenalId"));
        d.nom = limiter(connecteur.texte(element, champs.get("nom")), 100);
        d.numero = entier(connecteur.texte(element, champs.get("numero")));
        d.codeBarre = limiter(connecteur.texte(element, champs.get("codeBarre")), 100);
        Boolean actif = booleen(connecteur.texte(element, champs.get("actif")));
        d.actif = actif == null || actif;
        return d;
    }

    private List<DonneeEmplacement> dedoublonner(List<DonneeEmplacement> recus) {
        Map<String, DonneeEmplacement> uniques = new LinkedHashMap<>();
        for (DonneeEmplacement d : recus) {
            uniques.putIfAbsent(d.idExterne, d);
        }
        return List.copyOf(uniques.values());
    }

    private int[] appliquer(List<DonneeEmplacement> recus) {
        Map<String, Arsenal> arsenaux = arsenalRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(a -> a.getIdExterne() != null)
                .collect(Collectors.toMap(Arsenal::getIdExterne, Function.identity(), (a, b) -> a));

        Map<String, Emplacement> existants = emplacementRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(e -> e.getIdExterne() != null)
                .collect(Collectors.toMap(Emplacement::getIdExterne, Function.identity(), (a, b) -> a));

        int crees = 0;
        int misAJour = 0;
        int desactives = 0;
        int sansArsenal = 0;
        int conflits = 0;
        LocalDateTime maintenant = LocalDateTime.now();

        for (DonneeEmplacement d : recus) {
            Arsenal parent = d.arsenalIdExterne == null ? null : arsenaux.get(d.arsenalIdExterne);
            if (parent == null) {
                sansArsenal++;
                continue;
            }

            Emplacement emplacement = existants.get(d.idExterne);

            if (emplacement == null) {
                if (d.codeBarre != null && emplacementRepository.existsByCodeBarre(d.codeBarre)) {
                    conflits++;
                    continue;
                }
                emplacementRepository.save(Emplacement.builder()
                        .source(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .arsenal(parent)
                        .nom(d.nom)
                        .numero(d.numero)
                        .codeBarre(d.codeBarre)
                        .actif(d.actif)
                        .dateSynchro(maintenant)
                        .build());
                crees++;
                continue;
            }

            boolean modifie = false;

            if (!Objects.equals(emplacement.getArsenal().getId(), parent.getId())) {
                emplacement.setArsenal(parent);
                modifie = true;
            }
            if (d.nom != null && !Objects.equals(emplacement.getNom(), d.nom)) {
                emplacement.setNom(d.nom);
                modifie = true;
            }
            if (d.numero != null && !Objects.equals(emplacement.getNumero(), d.numero)) {
                emplacement.setNumero(d.numero);
                modifie = true;
            }
            if (d.codeBarre != null && !Objects.equals(emplacement.getCodeBarre(), d.codeBarre)) {
                boolean pris = emplacementRepository.findByCodeBarre(d.codeBarre)
                        .filter(autre -> !autre.getId().equals(emplacement.getId()))
                        .isPresent();
                if (pris) {
                    conflits++;
                } else {
                    emplacement.setCodeBarre(d.codeBarre);
                    modifie = true;
                }
            }
            if (!Objects.equals(emplacement.getActif(), d.actif)) {
                emplacement.setActif(d.actif);
                modifie = true;
            }

            if (modifie) {
                emplacement.setDateSynchro(maintenant);
                emplacementRepository.save(emplacement);
                misAJour++;
            }
        }

        Set<String> idsRecus = recus.stream().map(d -> d.idExterne).collect(Collectors.toSet());
        for (Emplacement emplacement : existants.values()) {
            if (!idsRecus.contains(emplacement.getIdExterne()) && !Boolean.FALSE.equals(emplacement.getActif())) {
                emplacement.setActif(false);
                emplacement.setDateSynchro(maintenant);
                emplacementRepository.save(emplacement);
                desactives++;
            }
        }

        return new int[]{crees, misAJour, desactives, sansArsenal, conflits};
    }

    private Boolean booleen(String valeur) {
        if (valeur == null) {
            return null;
        }
        return switch (valeur.trim().toLowerCase()) {
            case "true", "1", "oui", "yes", "vrai", "actif" -> true;
            case "false", "0", "non", "no", "faux", "inactif" -> false;
            default -> null;
        };
    }

    private Integer entier(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return Integer.parseInt(valeur.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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

    private static class DonneeEmplacement {
        private String idExterne;
        private String arsenalIdExterne;
        private String nom;
        private Integer numero;
        private String codeBarre;
        private boolean actif;
    }
}
