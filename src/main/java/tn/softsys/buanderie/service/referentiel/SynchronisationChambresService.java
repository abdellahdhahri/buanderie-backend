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
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.repository.ServiceRepository;

@Slf4j
@Service
public class SynchronisationChambresService {

    private static final int TAILLE_PAGE = 200;

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final ChambreRepository chambreRepository;
    private final ServiceRepository serviceRepository;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationChambresService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            ChambreRepository chambreRepository,
            ServiceRepository serviceRepository,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.chambreRepository = chambreRepository;
        this.serviceRepository = serviceRepository;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.CHAMBRE)) {
            throw new BusinessException("Les chambres ne sont pas importées : mode interne ou URL inactive.");
        }
        return recuperer(1).size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.CHAMBRE)) {
                throw new BusinessException("Les chambres sont gérées en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.CHAMBRE)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                List<DonneeChambre> recus = dedoublonner(recuperer(0));

                if (recus.isEmpty()) {
                    throw new BusinessException(
                            "La source n'a renvoyé aucune chambre exploitable : synchronisation annulée.");
                }

                int[] c = transactionTemplate.execute(status -> appliquer(recus));

                journal.setNbRecus(recus.size());
                journal.setNbCrees(c[0]);
                journal.setNbMisAJour(c[1]);
                journal.setNbDesactives(c[2]);
                journal.setStatut("SUCCES");

                StringBuilder message = new StringBuilder()
                        .append(recus.size()).append(" reçues, ")
                        .append(c[0]).append(" créées, ")
                        .append(c[1]).append(" mises à jour, ")
                        .append(c[2]).append(" désactivées");
                if (c[3] > 0) {
                    message.append(", ").append(c[3]).append(" ignorée(s) (code-barres déjà utilisé)");
                }
                message.append(".");
                journal.setMessage(message.toString());
            } catch (Exception e) {
                log.error("Synchronisation des chambres échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.CHAMBRE, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeChambre> recuperer(int pagesMax) {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.CHAMBRE);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des chambres n'est pas configurée.");
        }

        ConfigurationIntegration config = integrationService.getConfiguration();
        String url = integrationService.urlComplete(Referentiel.CHAMBRE);

        List<JsonNode> elements = config.getTypeConnecteur() == TypeConnecteur.CUBIX
                ? connecteur.recupererListePaginee(url, config, endpoint.getCheminListe(), TAILLE_PAGE, pagesMax)
                : connecteur.recupererListe(url, config, endpoint.getCheminListe());

        return elements.stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private DonneeChambre convertir(JsonNode element, Map<String, String> champs) {
        DonneeChambre d = new DonneeChambre();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.code = connecteur.texte(element, champs.get("code"));
        d.codeBarre = connecteur.texte(element, champs.get("codeBarre"));
        d.serviceIdExterne = connecteur.texte(element, champs.get("serviceId"));
        Boolean actif = booleen(connecteur.texte(element, champs.get("actif")));
        d.actif = actif == null || actif;
        return d;
    }

    private List<DonneeChambre> dedoublonner(List<DonneeChambre> recus) {
        Map<String, DonneeChambre> uniques = new LinkedHashMap<>();
        for (DonneeChambre d : recus) {
            uniques.putIfAbsent(d.idExterne, d);
        }
        return List.copyOf(uniques.values());
    }

    private int[] appliquer(List<DonneeChambre> recus) {
        Map<String, Chambre> existants = chambreRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(c -> c.getIdExterne() != null)
                .collect(Collectors.toMap(Chambre::getIdExterne, Function.identity(), (a, b) -> a));

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

        for (DonneeChambre d : recus) {
            Long serviceId = resoudreService(d.serviceIdExterne, servicesExternes, servicesConnus);
            String codeBarre = d.codeBarre;
            Chambre chambre = existants.get(d.idExterne);

            if (chambre == null) {
                if (codeBarre != null && chambreRepository.existsByCodeBarre(codeBarre)) {
                    conflits++;
                    continue;
                }
                chambreRepository.save(Chambre.builder()
                        .source(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .code(d.code)
                        .codeBarre(codeBarre)
                        .serviceId(serviceId)
                        .actif(d.actif)
                        .dateSynchro(maintenant)
                        .build());
                crees++;
                continue;
            }

            boolean modifie = false;

            if (d.code != null && !Objects.equals(chambre.getCode(), d.code)) {
                chambre.setCode(d.code);
                modifie = true;
            }
            if (serviceId != null && !Objects.equals(chambre.getServiceId(), serviceId)) {
                chambre.setServiceId(serviceId);
                modifie = true;
            }
            if (codeBarre != null && !Objects.equals(chambre.getCodeBarre(), codeBarre)) {
                boolean pris = chambreRepository.findByCodeBarre(codeBarre)
                        .filter(autre -> !autre.getId().equals(chambre.getId()))
                        .isPresent();
                if (pris) {
                    conflits++;
                } else {
                    chambre.setCodeBarre(codeBarre);
                    modifie = true;
                }
            }
            if (!Objects.equals(chambre.getActif(), d.actif)) {
                chambre.setActif(d.actif);
                modifie = true;
            }

            if (modifie) {
                chambre.setDateSynchro(maintenant);
                chambreRepository.save(chambre);
                misAJour++;
            }
        }

        Set<String> idsRecus = recus.stream().map(d -> d.idExterne).collect(Collectors.toSet());
        for (Chambre chambre : existants.values()) {
            if (!idsRecus.contains(chambre.getIdExterne()) && !Boolean.FALSE.equals(chambre.getActif())) {
                chambre.setActif(false);
                chambre.setDateSynchro(maintenant);
                chambreRepository.save(chambre);
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
        try {
            Long numerique = Long.parseLong(idExterne.trim());
            return servicesConnus.contains(numerique) ? numerique : null;
        } catch (NumberFormatException e) {
            return null;
        }
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

    private String tronquer(String message) {
        if (message == null) {
            return "Erreur inconnue";
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    private static class DonneeChambre {
        private String idExterne;
        private String code;
        private String codeBarre;
        private String serviceIdExterne;
        private boolean actif;
    }
}
