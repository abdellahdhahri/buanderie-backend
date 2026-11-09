package tn.softsys.buanderie.service.referentiel;

import java.time.LocalDateTime;
import java.util.HashSet;
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
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.repository.ServiceRepository;

@Slf4j
@Service
public class SynchronisationServicesService {

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final ServiceRepository serviceRepository;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationServicesService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            ServiceRepository serviceRepository,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.serviceRepository = serviceRepository;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.SERVICE)) {
            throw new BusinessException("Les services ne sont pas importés : mode interne ou URL inactive.");
        }
        return recuperer().size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.SERVICE)) {
                throw new BusinessException("Les services sont gérés en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.SERVICE)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                List<DonneeService> recus = recuperer();

                if (recus.isEmpty()) {
                    throw new BusinessException(
                            "La source n'a renvoyé aucun service exploitable : synchronisation annulée pour ne pas tout désactiver.");
                }

                int[] compteurs = transactionTemplate.execute(status -> appliquer(recus));

                journal.setNbRecus(recus.size());
                journal.setNbCrees(compteurs[0]);
                journal.setNbMisAJour(compteurs[1]);
                journal.setNbDesactives(compteurs[2]);
                journal.setStatut("SUCCES");
                journal.setMessage(recus.size() + " reçus, " + compteurs[0] + " créés, "
                        + compteurs[1] + " mis à jour, " + compteurs[2] + " désactivés.");
            } catch (Exception e) {
                log.error("Synchronisation des services échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.SERVICE, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeService> recuperer() {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.SERVICE);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des services n'est pas configurée.");
        }

        List<JsonNode> elements = connecteur.recupererListe(
                integrationService.urlComplete(Referentiel.SERVICE),
                integrationService.getConfiguration(),
                endpoint.getCheminListe());

        return elements.stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private DonneeService convertir(JsonNode element, Map<String, String> champs) {
        DonneeService d = new DonneeService();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.code = connecteur.texte(element, champs.get("code"));
        d.nom = connecteur.texte(element, champs.get("nom"));
        d.description = connecteur.texte(element, champs.get("description"));
        d.reference = connecteur.texte(element, champs.get("reference"));

        Boolean actif = booleen(connecteur.texte(element, champs.get("actif")));
        d.actif = actif == null || actif;

        if (d.nom == null) {
            d.nom = d.description != null ? d.description : d.code;
        }
        return d;
    }

    private int[] appliquer(List<DonneeService> recus) {
        Map<String, ServiceEntity> existants = serviceRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(s -> s.getIdExterne() != null)
                .collect(Collectors.toMap(ServiceEntity::getIdExterne, Function.identity(), (a, b) -> a));

        Set<String> vus = new HashSet<>();
        int crees = 0;
        int misAJour = 0;
        LocalDateTime maintenant = LocalDateTime.now();

        for (DonneeService d : recus) {
            if (!vus.add(d.idExterne)) {
                continue;
            }

            ServiceEntity service = existants.get(d.idExterne);

            if (service == null) {
                service = ServiceEntity.builder()
                        .source(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .build();
                crees++;
            } else if (!identique(service, d)) {
                misAJour++;
            } else {
                service.setDateSynchro(maintenant);
                serviceRepository.save(service);
                continue;
            }

            service.setCodeService(d.code);
            service.setNom(d.nom);
            service.setDescription(d.description);
            service.setReference(d.reference);
            service.setActif(d.actif);
            service.setDateSynchro(maintenant);
            serviceRepository.save(service);
        }

        int desactives = 0;
        for (ServiceEntity service : existants.values()) {
            if (!vus.contains(service.getIdExterne()) && Boolean.TRUE.equals(service.getActif())) {
                service.setActif(false);
                service.setDateSynchro(maintenant);
                serviceRepository.save(service);
                desactives++;
            }
        }

        return new int[]{crees, misAJour, desactives};
    }

    private boolean identique(ServiceEntity s, DonneeService d) {
        return Objects.equals(s.getCodeService(), d.code)
                && Objects.equals(s.getNom(), d.nom)
                && Objects.equals(s.getDescription(), d.description)
                && Objects.equals(s.getReference(), d.reference)
                && Objects.equals(s.getActif(), d.actif);
    }

    private Boolean booleen(String valeur) {
        if (valeur == null) {
            return null;
        }
        String v = valeur.trim().toLowerCase();
        if (v.equals("true") || v.equals("1") || v.equals("oui") || v.equals("yes")) {
            return true;
        }
        if (v.equals("false") || v.equals("0") || v.equals("non") || v.equals("no")) {
            return false;
        }
        return null;
    }

    private String tronquer(String message) {
        if (message == null) {
            return "Erreur inconnue";
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    private static class DonneeService {
        private String idExterne;
        private String code;
        private String nom;
        private String description;
        private String reference;
        private boolean actif;
    }
}