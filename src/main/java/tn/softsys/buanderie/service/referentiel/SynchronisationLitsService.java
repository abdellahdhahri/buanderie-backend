package tn.softsys.buanderie.service.referentiel;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.Lit;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.repository.LitRepository;

@Slf4j
@Service
public class SynchronisationLitsService {

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final LitRepository litRepository;
    private final ChambreRepository chambreRepository;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationLitsService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            LitRepository litRepository,
            ChambreRepository chambreRepository,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.litRepository = litRepository;
        this.chambreRepository = chambreRepository;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.LIT)) {
            throw new BusinessException("Les lits ne sont pas importés : mode interne ou URL inactive.");
        }
        return recuperer(true).size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.LIT)) {
                throw new BusinessException("Les lits sont gérés en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.LIT)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                List<DonneeLit> recus = dedoublonner(recuperer(false));

                if (recus.isEmpty()) {
                    throw new BusinessException(
                            "La source n'a renvoyé aucun lit exploitable : synchronisation annulée.");
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
                            .append(" ignorés (chambre introuvable : synchronisez d'abord les chambres)");
                }
                message.append(".");
                journal.setMessage(message.toString());
            } catch (Exception e) {
                log.error("Synchronisation des lits échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.LIT, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeLit> recuperer(boolean test) {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.LIT);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des lits n'est pas configurée.");
        }
        if (champs.get("chambreId") == null) {
            throw new BusinessException("La correspondance du champ « chambreId » des lits n'est pas configurée.");
        }

        ConfigurationIntegration config = integrationService.getConfiguration();
        String url = integrationService.urlComplete(Referentiel.LIT);

        if (config.getTypeConnecteur() == TypeConnecteur.CUBIX) {
            return recupererParChambre(url, config, endpoint, champs, test);
        }

        return connecteur.recupererListe(url, config, endpoint.getCheminListe()).stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private List<DonneeLit> recupererParChambre(String url, ConfigurationIntegration config,
                                                EndpointIntegration endpoint, Map<String, String> champs,
                                                boolean test) {
        List<Chambre> chambres = chambreRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(c -> c.getIdExterne() != null && !Boolean.FALSE.equals(c.getActif()))
                .toList();

        if (chambres.isEmpty()) {
            throw new BusinessException("Aucune chambre synchronisée : synchronisez d'abord les chambres.");
        }

        List<Chambre> cibles = test ? chambres.subList(0, 1) : chambres;
        List<DonneeLit> resultat = new ArrayList<>();
        int traitees = 0;

        for (Chambre chambre : cibles) {
            String urlChambre = UriComponentsBuilder.fromUriString(url)
                    .replaceQueryParam("chambreId", chambre.getIdExterne())
                    .build()
                    .toUriString();

            for (JsonNode element : connecteur.recupererListe(urlChambre, config, endpoint.getCheminListe())) {
                DonneeLit d = convertir(element, champs);
                if (d.chambreIdExterne == null) {
                    d.chambreIdExterne = chambre.getIdExterne();
                }
                if (d.idExterne != null) {
                    resultat.add(d);
                }
            }

            traitees++;
            if (traitees % 50 == 0) {
                log.info("Synchronisation des lits : {}/{} chambres lues", traitees, cibles.size());
            }
        }

        return resultat;
    }

    private DonneeLit convertir(JsonNode element, Map<String, String> champs) {
        DonneeLit d = new DonneeLit();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.chambreIdExterne = connecteur.texte(element, champs.get("chambreId"));
        d.code = limiter(connecteur.texte(element, champs.get("code")), 100);
        d.description = limiter(connecteur.texte(element, champs.get("description")), 255);
        d.type = limiter(connecteur.texte(element, champs.get("type")), 20);
        Boolean actif = booleen(connecteur.texte(element, champs.get("actif")));
        d.actif = actif == null || actif;
        return d;
    }

    private List<DonneeLit> dedoublonner(List<DonneeLit> recus) {
        Map<String, DonneeLit> uniques = new LinkedHashMap<>();
        for (DonneeLit d : recus) {
            uniques.putIfAbsent(d.idExterne, d);
        }
        return List.copyOf(uniques.values());
    }

    private int[] appliquer(List<DonneeLit> recus) {
        Map<String, Chambre> chambres = chambreRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(c -> c.getIdExterne() != null)
                .collect(Collectors.toMap(Chambre::getIdExterne, Function.identity(), (a, b) -> a));

        Map<String, Lit> existants = litRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(l -> l.getIdExterne() != null)
                .collect(Collectors.toMap(Lit::getIdExterne, Function.identity(), (a, b) -> a));

        int crees = 0;
        int misAJour = 0;
        int desactives = 0;
        int sansChambre = 0;
        LocalDateTime maintenant = LocalDateTime.now();

        for (DonneeLit d : recus) {
            Chambre parent = d.chambreIdExterne == null ? null : chambres.get(d.chambreIdExterne);
            if (parent == null) {
                sansChambre++;
                continue;
            }

            Lit lit = existants.get(d.idExterne);

            if (lit == null) {
                litRepository.save(Lit.builder()
                        .source(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .chambre(parent)
                        .code(d.code)
                        .description(d.description)
                        .reference(referenceDe(d.code))
                        .type(d.type != null ? d.type.toUpperCase() : "SIMPLE")
                        .actif(d.actif)
                        .infecte(false)
                        .dateSynchro(maintenant)
                        .build());
                crees++;
                continue;
            }

            boolean modifie = false;

            if (!Objects.equals(lit.getChambre().getId(), parent.getId())) {
                lit.setChambre(parent);
                modifie = true;
            }
            if (d.code != null && !Objects.equals(lit.getCode(), d.code)) {
                lit.setCode(d.code);
                modifie = true;
            }
            if (d.description != null && !Objects.equals(lit.getDescription(), d.description)) {
                lit.setDescription(d.description);
                modifie = true;
            }
            if (d.type != null && !Objects.equals(lit.getType(), d.type.toUpperCase())) {
                lit.setType(d.type.toUpperCase());
                modifie = true;
            }
            if (!Objects.equals(lit.getActif(), d.actif)) {
                lit.setActif(d.actif);
                modifie = true;
            }

            if (modifie) {
                lit.setDateSynchro(maintenant);
                litRepository.save(lit);
                misAJour++;
            }
        }

        Set<String> idsRecus = recus.stream().map(d -> d.idExterne).collect(Collectors.toSet());
        for (Lit lit : existants.values()) {
            if (!idsRecus.contains(lit.getIdExterne()) && !Boolean.FALSE.equals(lit.getActif())) {
                lit.setActif(false);
                lit.setDateSynchro(maintenant);
                litRepository.save(lit);
                desactives++;
            }
        }

        return new int[]{crees, misAJour, desactives, sansChambre};
    }

    private String referenceDe(String code) {
        if (code != null && !code.isBlank()) {
            return code.length() > 20 ? code.substring(0, 20) : code;
        }
        return "LIT" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
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

    private static class DonneeLit {
        private String idExterne;
        private String chambreIdExterne;
        private String code;
        private String description;
        private String type;
        private boolean actif;
    }
}
