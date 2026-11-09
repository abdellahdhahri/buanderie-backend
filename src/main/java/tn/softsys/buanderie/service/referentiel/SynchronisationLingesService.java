package tn.softsys.buanderie.service.referentiel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.ServiceRepository;

@Slf4j
@Service
public class SynchronisationLingesService {

    private static final int TAILLE_PAGE = 200;
    private static final int TAILLE_LOT = 500;

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final LingeRepository lingeRepository;
    private final ServiceRepository serviceRepository;
    private final CategLingeResolver categLingeResolver;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationLingesService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            LingeRepository lingeRepository,
            ServiceRepository serviceRepository,
            CategLingeResolver categLingeResolver,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.lingeRepository = lingeRepository;
        this.serviceRepository = serviceRepository;
        this.categLingeResolver = categLingeResolver;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.LINGE)) {
            throw new BusinessException("Les linges ne sont pas importés : mode interne ou URL inactive.");
        }
        return recuperer(1).size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.LINGE)) {
                throw new BusinessException("Les linges sont gérés en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.LINGE)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            int crees = 0;
            int misAJour = 0;
            int ignores = 0;
            int nbRecus = 0;

            try {
                List<DonneeLinge> recus = dedoublonner(recuperer(0));
                nbRecus = recus.size();

                if (recus.isEmpty()) {
                    throw new BusinessException(
                            "La source n'a renvoyé aucun linge exploitable : synchronisation annulée.");
                }

                resoudreServices(recus);

                for (int debut = 0; debut < recus.size(); debut += TAILLE_LOT) {
                    List<DonneeLinge> lot = recus.subList(debut, Math.min(debut + TAILLE_LOT, recus.size()));
                    int[] c = transactionTemplate.execute(status -> appliquerLot(lot));
                    crees += c[0];
                    misAJour += c[1];
                    ignores += c[2];
                    log.info("Synchronisation des linges : {}/{} traités ({} créés, {} mis à jour)",
                            Math.min(debut + TAILLE_LOT, recus.size()), recus.size(), crees, misAJour);
                }

                Set<String> idsRecus = recus.stream().map(d -> d.idExterne).collect(Collectors.toSet());
                long absents = lingeRepository.findIdsExternes(SourceDonnee.EXTERNE).stream()
                        .filter(id -> !idsRecus.contains(id))
                        .count();

                journal.setStatut("SUCCES");

                StringBuilder message = new StringBuilder()
                        .append(nbRecus).append(" reçus, ")
                        .append(crees).append(" créés, ")
                        .append(misAJour).append(" mis à jour");
                if (ignores > 0) {
                    message.append(", ").append(ignores).append(" ignorés (code-barres manquant ou déjà utilisé)");
                }
                if (absents > 0) {
                    message.append(", ").append(absents).append(" absents de la source (conservés)");
                }
                message.append(".");
                journal.setMessage(message.toString());
            } catch (Exception e) {
                log.error("Synchronisation des linges échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()
                        + (crees + misAJour > 0
                        ? " (déjà appliqués avant l'échec : " + crees + " créés, " + misAJour + " mis à jour)"
                        : "")));
            }

            journal.setNbRecus(nbRecus);
            journal.setNbCrees(crees);
            journal.setNbMisAJour(misAJour);
            journal.setNbDesactives(0);
            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.LINGE, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeLinge> recuperer(int pagesMax) {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.LINGE);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des linges n'est pas configurée.");
        }
        if (champs.get("codeBarre") == null) {
            throw new BusinessException("La correspondance du champ « codeBarre » des linges n'est pas configurée.");
        }

        ConfigurationIntegration config = integrationService.getConfiguration();
        String url = integrationService.urlComplete(Referentiel.LINGE);

        List<JsonNode> elements = config.getTypeConnecteur() == TypeConnecteur.CUBIX
                ? connecteur.recupererListePaginee(url, config, endpoint.getCheminListe(), TAILLE_PAGE, pagesMax)
                : connecteur.recupererListe(url, config, endpoint.getCheminListe());

        return elements.stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private DonneeLinge convertir(JsonNode element, Map<String, String> champs) {
        DonneeLinge d = new DonneeLinge();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.designation = connecteur.texte(element, champs.get("designation"));
        d.codeBarre = connecteur.texte(element, champs.get("codeBarre"));
        d.etatImmobilisation = etat(connecteur.texte(element, champs.get("etatActif")));
        d.categorie = categorie(connecteur.texte(element, champs.get("categorie")));
        d.serviceIdExterne = connecteur.texte(element, champs.get("serviceId"));
        return d;
    }

    private List<DonneeLinge> dedoublonner(List<DonneeLinge> recus) {
        Map<String, DonneeLinge> uniques = new LinkedHashMap<>();
        for (DonneeLinge d : recus) {
            uniques.putIfAbsent(d.idExterne, d);
        }
        return new ArrayList<>(uniques.values());
    }

    private void resoudreServices(List<DonneeLinge> recus) {
        Map<String, Long> servicesExternes = serviceRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(sv -> sv.getIdExterne() != null)
                .collect(Collectors.toMap(ServiceEntity::getIdExterne, ServiceEntity::getId, (a, b) -> a));

        Set<Long> servicesConnus = serviceRepository.findAll().stream()
                .map(ServiceEntity::getId)
                .collect(Collectors.toSet());

        for (DonneeLinge d : recus) {
            if (d.serviceIdExterne == null) {
                continue;
            }
            Long interne = servicesExternes.get(d.serviceIdExterne);
            if (interne == null) {
                Long numerique = nombre(d.serviceIdExterne);
                interne = numerique != null && servicesConnus.contains(numerique) ? numerique : null;
            }
            d.serviceId = interne;
        }
    }

    private int[] appliquerLot(List<DonneeLinge> lot) {
        List<String> ids = lot.stream().map(d -> d.idExterne).toList();

        Map<String, Linge> existants = lingeRepository.findBySourceAndIdExterneIn(SourceDonnee.EXTERNE, ids).stream()
                .collect(Collectors.toMap(Linge::getIdExterne, Function.identity(), (a, b) -> a));

        Set<String> codes = lot.stream()
                .map(d -> d.codeBarre)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Long> codesPris = new HashMap<>();
        if (!codes.isEmpty()) {
            lingeRepository.findByCodeBarreIn(codes)
                    .forEach(l -> codesPris.putIfAbsent(l.getCodeBarre(), l.getId()));
        }

        List<Linge> aSauver = new ArrayList<>();
        int crees = 0;
        int misAJour = 0;
        int ignores = 0;
        LocalDateTime maintenant = LocalDateTime.now();

        for (DonneeLinge d : lot) {
            Linge linge = existants.get(d.idExterne);

            if (linge == null) {
                if (d.codeBarre == null || codesPris.containsKey(d.codeBarre)) {
                    ignores++;
                    continue;
                }

                CategLinge categorie = d.categorie != null
                        ? d.categorie
                        : categLingeResolver.resoudre(d.designation);

                aSauver.add(Linge.builder()
                        .source(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .codeBarre(d.codeBarre)
                        .designation(d.designation)
                        .etatImmobilisation(d.etatImmobilisation)
                        .categorie(categorie)
                        .serviceId(d.serviceId)
                        .dateSynchro(maintenant)
                        .build());
                codesPris.put(d.codeBarre, -1L);
                crees++;
                continue;
            }

            boolean modifie = false;

            if (d.designation != null && !Objects.equals(linge.getDesignation(), d.designation)) {
                linge.setDesignation(d.designation);
                modifie = true;
            }
            if (d.etatImmobilisation != null && linge.getEtatImmobilisation() != d.etatImmobilisation) {
                linge.setEtatImmobilisation(d.etatImmobilisation);
                modifie = true;
            }
            if (d.categorie != null && linge.getCategorie() != d.categorie) {
                linge.setCategorie(d.categorie);
                modifie = true;
            }
            if (d.serviceId != null && !Objects.equals(linge.getServiceId(), d.serviceId)) {
                linge.setServiceId(d.serviceId);
                modifie = true;
            }
            if (d.codeBarre != null && !Objects.equals(linge.getCodeBarre(), d.codeBarre)) {
                Long proprietaire = codesPris.get(d.codeBarre);
                if (proprietaire != null && !proprietaire.equals(linge.getId())) {
                    ignores++;
                } else {
                    linge.setCodeBarre(d.codeBarre);
                    codesPris.put(d.codeBarre, linge.getId());
                    modifie = true;
                }
            }

            if (modifie) {
                linge.setDateSynchro(maintenant);
                aSauver.add(linge);
                misAJour++;
            }
        }

        lingeRepository.saveAll(aSauver);

        return new int[]{crees, misAJour, ignores};
    }

    private EtatImmobilisation etat(String valeur) {
        if (valeur == null) {
            return null;
        }
        Long code = nombre(valeur);
        if (code != null) {
            return switch (code.intValue()) {
                case 0 -> EtatImmobilisation.DISPONIBLE;
                case 4 -> EtatImmobilisation.EN_COURS;
                case 1 -> EtatImmobilisation.VENDU;
                case 2 -> EtatImmobilisation.PERDU;
                case 5 -> EtatImmobilisation.MISE_AU_REBUT;
                case 6 -> EtatImmobilisation.VOLE;
                case 10 -> EtatImmobilisation.ANNULE;
                default -> null;
            };
        }
        try {
            return EtatImmobilisation.valueOf(normaliser(valeur));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private CategLinge categorie(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return CategLinge.valueOf(normaliser(valeur));
        } catch (IllegalArgumentException e) {
            CategLinge suggeree = categLingeResolver.resoudre(valeur);
            return suggeree == CategLinge.AUTRE ? null : suggeree;
        }
    }

    private String normaliser(String valeur) {
        return valeur.trim().toUpperCase().replace(' ', '_').replace('-', '_');
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

    private String tronquer(String message) {
        if (message == null) {
            return "Erreur inconnue";
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    private static class DonneeLinge {
        private String idExterne;
        private String designation;
        private String codeBarre;
        private EtatImmobilisation etatImmobilisation;
        private CategLinge categorie;
        private String serviceIdExterne;
        private Long serviceId;
    }
}
