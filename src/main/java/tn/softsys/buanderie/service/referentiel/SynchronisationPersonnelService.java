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
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.repository.ServiceRepository;

@Slf4j
@Service
public class SynchronisationPersonnelService {

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final PersonnelRepository personnelRepository;
    private final ServiceRepository serviceRepository;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationPersonnelService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            PersonnelRepository personnelRepository,
            ServiceRepository serviceRepository,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.personnelRepository = personnelRepository;
        this.serviceRepository = serviceRepository;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.PERSONNEL)) {
            throw new BusinessException("Le personnel n'est pas importé : mode interne ou URL inactive.");
        }
        return recuperer().size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.PERSONNEL)) {
                throw new BusinessException("Le personnel est géré en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.PERSONNEL)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                List<DonneePersonnel> recus = recuperer();

                if (recus.isEmpty()) {
                    throw new BusinessException(
                            "La source n'a renvoyé aucun personnel exploitable : synchronisation annulée pour ne pas tout désactiver.");
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
                log.error("Synchronisation du personnel échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.PERSONNEL, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneePersonnel> recuperer() {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.PERSONNEL);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » du personnel n'est pas configurée.");
        }

        List<JsonNode> elements = connecteur.recupererListe(
                integrationService.urlComplete(Referentiel.PERSONNEL),
                integrationService.getConfiguration(),
                endpoint.getCheminListe());

        return elements.stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private DonneePersonnel convertir(JsonNode element, Map<String, String> champs) {
        DonneePersonnel d = new DonneePersonnel();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.nom = connecteur.texte(element, champs.get("nom"));
        d.prenom = connecteur.texte(element, champs.get("prenom"));
        d.matricule = connecteur.texte(element, champs.get("matricule"));
        d.codeBarre = connecteur.texte(element, champs.get("codeBarre"));
        d.fonction = connecteur.texte(element, champs.get("fonction"));
        d.fonctionId = nombre(connecteur.texte(element, champs.get("fonctionId")));
        d.serviceIdExterne = connecteur.texte(element, champs.get("serviceId"));

        Boolean actif = booleen(connecteur.texte(element, champs.get("actif")));
        Boolean masque = booleen(connecteur.texte(element, champs.get("masque")));
        d.actif = actif != null ? actif : !Boolean.TRUE.equals(masque);
        return d;
    }

    private int[] appliquer(List<DonneePersonnel> recus) {
        Map<String, Personnel> existants = personnelRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(p -> p.getIdExterne() != null)
                .collect(Collectors.toMap(Personnel::getIdExterne, Function.identity(), (a, b) -> a));

        Map<String, Long> servicesInternes = serviceRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(sv -> sv.getIdExterne() != null)
                .collect(Collectors.toMap(sv -> sv.getIdExterne(), sv -> sv.getId(), (a, b) -> a));

        for (DonneePersonnel d : recus) {
            d.serviceId = d.serviceIdExterne == null
                    ? null
                    : servicesInternes.getOrDefault(d.serviceIdExterne, nombre(d.serviceIdExterne));
        }

        Set<String> vus = new HashSet<>();
        int crees = 0;
        int misAJour = 0;
        LocalDateTime maintenant = LocalDateTime.now();

        for (DonneePersonnel d : recus) {
            if (!vus.add(d.idExterne)) {
                continue;
            }

            Personnel personnel = existants.get(d.idExterne);

            if (personnel == null) {
                personnel = Personnel.builder()
                        .source(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .build();
                crees++;
            } else if (!identique(personnel, d)) {
                misAJour++;
            } else {
                personnel.setDateSynchro(maintenant);
                personnelRepository.save(personnel);
                continue;
            }

            personnel.setNom(d.nom);
            personnel.setPrenom(d.prenom);
            personnel.setMatricule(d.matricule);
            personnel.setCodeBarre(d.codeBarre);
            personnel.setFonction(d.fonction);
            personnel.setFonctionId(d.fonctionId);
            personnel.setServiceId(d.serviceId);
            personnel.setActif(d.actif);
            personnel.setDateSynchro(maintenant);
            personnelRepository.save(personnel);
        }

        int desactives = 0;
        for (Personnel personnel : existants.values()) {
            if (!vus.contains(personnel.getIdExterne()) && Boolean.TRUE.equals(personnel.getActif())) {
                personnel.setActif(false);
                personnel.setDateSynchro(maintenant);
                personnelRepository.save(personnel);
                desactives++;
            }
        }

        return new int[]{crees, misAJour, desactives};
    }

    private boolean identique(Personnel p, DonneePersonnel d) {
        return Objects.equals(p.getNom(), d.nom)
                && Objects.equals(p.getPrenom(), d.prenom)
                && Objects.equals(p.getMatricule(), d.matricule)
                && Objects.equals(p.getCodeBarre(), d.codeBarre)
                && Objects.equals(p.getFonction(), d.fonction)
                && Objects.equals(p.getFonctionId(), d.fonctionId)
                && Objects.equals(p.getServiceId(), d.serviceId)
                && Objects.equals(p.getActif(), d.actif);
    }

    private Long nombre(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return Long.parseLong(valeur.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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

    private static class DonneePersonnel {
        private String idExterne;
        private String nom;
        private String prenom;
        private String matricule;
        private String codeBarre;
        private String fonction;
        private Long fonctionId;
        private String serviceIdExterne;
        private Long serviceId;
        private boolean actif;
    }
}