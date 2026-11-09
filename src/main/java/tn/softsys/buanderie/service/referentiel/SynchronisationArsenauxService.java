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
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ArsenalRepository;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.repository.ServiceRepository;

@Slf4j
@Service
public class SynchronisationArsenauxService {

    private static final int TAILLE_PAGE = 200;

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final ArsenalRepository arsenalRepository;
    private final ServiceRepository serviceRepository;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationArsenauxService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            ArsenalRepository arsenalRepository,
            ServiceRepository serviceRepository,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.arsenalRepository = arsenalRepository;
        this.serviceRepository = serviceRepository;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.ARSENAL)) {
            throw new BusinessException("Les arsenaux ne sont pas importés : mode interne ou URL inactive.");
        }
        return recuperer(1).size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.ARSENAL)) {
                throw new BusinessException("Les arsenaux sont gérés en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.ARSENAL)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                List<DonneeArsenal> recus = dedoublonner(recuperer(0));

                if (recus.isEmpty()) {
                    throw new BusinessException(
                            "La source n'a renvoyé aucun arsenal exploitable : synchronisation annulée.");
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
                    message.append(", ").append(c[3]).append(" code(s) déjà utilisé(s) ignoré(s)");
                }
                message.append(".");
                journal.setMessage(message.toString());
            } catch (Exception e) {
                log.error("Synchronisation des arsenaux échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.ARSENAL, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeArsenal> recuperer(int pagesMax) {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.ARSENAL);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des arsenaux n'est pas configurée.");
        }
        if (champs.get("nom") == null) {
            throw new BusinessException("La correspondance du champ « nom » des arsenaux n'est pas configurée.");
        }

        ConfigurationIntegration config = integrationService.getConfiguration();
        String url = integrationService.urlComplete(Referentiel.ARSENAL);

        List<JsonNode> elements = config.getTypeConnecteur() == TypeConnecteur.CUBIX
                ? connecteur.recupererListePaginee(url, config, endpoint.getCheminListe(), TAILLE_PAGE, pagesMax)
                : connecteur.recupererListe(url, config, endpoint.getCheminListe());

        return elements.stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private DonneeArsenal convertir(JsonNode element, Map<String, String> champs) {
        DonneeArsenal d = new DonneeArsenal();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.nom = connecteur.texte(element, champs.get("nom"));
        d.code = connecteur.texte(element, champs.get("code"));
        d.serviceIdExterne = connecteur.texte(element, champs.get("serviceId"));
        Boolean actif = booleen(connecteur.texte(element, champs.get("actif")));
        d.actif = actif == null || actif;
        return d;
    }

    private List<DonneeArsenal> dedoublonner(List<DonneeArsenal> recus) {
        Map<String, DonneeArsenal> uniques = new LinkedHashMap<>();
        for (DonneeArsenal d : recus) {
            uniques.putIfAbsent(d.idExterne, d);
        }
        return List.copyOf(uniques.values());
    }

    private int[] appliquer(List<DonneeArsenal> recus) {
        Map<String, Arsenal> existants = arsenalRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(a -> a.getIdExterne() != null)
                .collect(Collectors.toMap(Arsenal::getIdExterne, Function.identity(), (a, b) -> a));

        Map<String, Long> servicesExternes = serviceRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(sv -> sv.getIdExterne() != null)
                .collect(Collectors.toMap(ServiceEntity::getIdExterne, ServiceEntity::getId, (a, b) -> a));

        Set<Long> servicesConnus = serviceRepository.findAll().stream()
                .map(ServiceEntity::getId)
                .collect(Collectors.toSet());

        int crees = 0;
        int misAJour = 0;
        int desactives = 0;
        int conflits = 0;
        LocalDateTime maintenant = LocalDateTime.now();

        for (DonneeArsenal d : recus) {
            Long serviceId = resoudreService(d.serviceIdExterne, servicesExternes, servicesConnus);
            String code = d.code;
            Arsenal arsenal = existants.get(d.idExterne);

            if (arsenal == null) {
                if (code != null && arsenalRepository.findByCode(code).isPresent()) {
                    code = null;
                    conflits++;
                }
                arsenalRepository.save(Arsenal.builder()
                        .source(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .nom(d.nom)
                        .code(code)
                        .serviceId(serviceId)
                        .actif(d.actif)
                        .dateSynchro(maintenant)
                        .build());
                crees++;
                continue;
            }

            boolean modifie = false;

            if (d.nom != null && !Objects.equals(arsenal.getNom(), d.nom)) {
                arsenal.setNom(d.nom);
                modifie = true;
            }
            if (code != null && !Objects.equals(arsenal.getCode(), code)) {
                boolean pris = arsenalRepository.findByCode(code)
                        .filter(autre -> !autre.getId().equals(arsenal.getId()))
                        .isPresent();
                if (pris) {
                    conflits++;
                } else {
                    arsenal.setCode(code);
                    modifie = true;
                }
            }
            if (serviceId != null && !Objects.equals(arsenal.getServiceId(), serviceId)) {
                arsenal.setServiceId(serviceId);
                modifie = true;
            }
            if (!Objects.equals(arsenal.getActif(), d.actif)) {
                arsenal.setActif(d.actif);
                modifie = true;
            }

            if (modifie) {
                arsenal.setDateSynchro(maintenant);
                arsenalRepository.save(arsenal);
                misAJour++;
            }
        }

        Set<String> idsRecus = recus.stream().map(d -> d.idExterne).collect(Collectors.toSet());
        for (Arsenal arsenal : existants.values()) {
            if (!idsRecus.contains(arsenal.getIdExterne()) && !Boolean.FALSE.equals(arsenal.getActif())) {
                arsenal.setActif(false);
                arsenal.setDateSynchro(maintenant);
                arsenalRepository.save(arsenal);
                desactives++;
            }
        }

        return new int[]{crees, misAJour, desactives, conflits};
    }

    private Long resoudreService(String idExterne, Map<String, Long> servicesExternes, Set<Long> servicesConnus) {
        if (idExterne == null) {
            return null;
        }
        Long interne = servicesExternes.get(idExterne);
        if (interne != null) {
            return interne;
        }
        Long numerique = nombre(idExterne);
        return numerique != null && servicesConnus.contains(numerique) ? numerique : null;
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

    private Long nombre(String valeur) {
        try {
            return Long.parseLong(valeur.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String tronquer(String message) {
        if (message == null) {
            return "Erreur inconnue";
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    private static class DonneeArsenal {
        private String idExterne;
        private String nom;
        private String code;
        private String serviceIdExterne;
        private boolean actif;
    }
}
