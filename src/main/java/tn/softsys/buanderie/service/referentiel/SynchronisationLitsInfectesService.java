package tn.softsys.buanderie.service.referentiel;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.Lit;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.repository.LitRepository;

@Slf4j
@Service
public class SynchronisationLitsInfectesService {

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final LitRepository litRepository;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    /** Au plus ce nombre de lits peut redevenir « non infecté » en une synchronisation, sans autre contrôle. */
    @org.springframework.beans.factory.annotation.Value("${lits-infectes.retraits-autorises:5}")
    private int retraitsAutorises;

    /** Au-delà, la synchronisation est refusée si plus de ce pourcentage des lits infectés est retiré d'un coup. */
    @org.springframework.beans.factory.annotation.Value("${lits-infectes.seuil-retrait-pct:50}")
    private int seuilRetraitPct;

    public SynchronisationLitsInfectesService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            LitRepository litRepository,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.litRepository = litRepository;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.LIT_INFECTE)) {
            throw new BusinessException("L'infection n'est pas importée : mode interne ou URL inactive.");
        }
        return recuperer().size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.LIT_INFECTE)) {
                throw new BusinessException("L'infection est gérée en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.LIT_INFECTE)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                Set<String> infectes = new HashSet<>();
                for (DonneeInfection d : recuperer()) {
                    if (d.infecte == null || d.infecte) {
                        infectes.add(d.idExterne);
                    }
                }

                List<String> retires = new java.util.ArrayList<>();
                int[] c = transactionTemplate.execute(status -> appliquer(infectes, retires));

                journal.setNbRecus(infectes.size());
                journal.setNbCrees(0);
                journal.setNbMisAJour(c[0]);
                journal.setNbDesactives(0);
                journal.setStatut("SUCCES");

                StringBuilder message = new StringBuilder()
                        .append(infectes.size()).append(" lit(s) infecté(s) dans la source, ")
                        .append(c[0]).append(" lit(s) modifié(s)");
                if (c[1] > 0) {
                    message.append(", ").append(c[1])
                            .append(" lit(s) infecté(s) inconnu(s) : synchronisez d'abord les lits");
                }
                message.append(".");
                if (!retires.isEmpty()) {
                    message.append(" ATTENTION : ").append(retires.size())
                            .append(" lit(s) redevenu(s) non infecté(s) : ")
                            .append(String.join(", ", retires.stream().limit(20).toList()))
                            .append(retires.size() > 20 ? "…" : "").append(".");
                }
                journal.setMessage(message.toString());
            } catch (Exception e) {
                log.error("Synchronisation de l'infection échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.LIT_INFECTE, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeInfection> recuperer() {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.LIT_INFECTE);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des lits infectés n'est pas configurée.");
        }

        ConfigurationIntegration config = integrationService.getConfiguration();
        String url = integrationService.urlComplete(Referentiel.LIT_INFECTE);

        List<JsonNode> elements = connecteur.recupererListe(url, config, endpoint.getCheminListe());

        return elements.stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private DonneeInfection convertir(JsonNode element, Map<String, String> champs) {
        DonneeInfection d = new DonneeInfection();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.infecte = booleen(connecteur.texte(element, champs.get("infecte")));
        return d;
    }

    private int[] appliquer(Set<String> infectes, List<String> retires) {
        List<Lit> lits = litRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(l -> l.getIdExterne() != null)
                .toList();

        long infectesActuels = lits.stream().filter(l -> Boolean.TRUE.equals(l.getInfecte())).count();
        List<Lit> aRetirer = lits.stream()
                .filter(l -> Boolean.TRUE.equals(l.getInfecte()) && !infectes.contains(l.getIdExterne()))
                .toList();

        long maxRetraits = Math.max(retraitsAutorises, infectesActuels * seuilRetraitPct / 100);
        if (aRetirer.size() > maxRetraits) {
            throw new BusinessException("Synchronisation des lits infectés refusée : " + aRetirer.size()
                    + " lit(s) sur " + infectesActuels + " redeviendraient non infectés d'un seul coup (limite : "
                    + maxRetraits + "). Vérifiez la source CUBIX ; aucun lit n'a été modifié.");
        }

        int modifies = 0;
        Set<String> connus = new HashSet<>();

        for (Lit lit : lits) {
            connus.add(lit.getIdExterne());

            boolean doitEtreInfecte = infectes.contains(lit.getIdExterne());
            if (!Objects.equals(lit.getInfecte(), doitEtreInfecte)) {
                if (Boolean.TRUE.equals(lit.getInfecte()) && !doitEtreInfecte) {
                    retires.add(lit.getCode() != null ? lit.getCode() : lit.getIdExterne());
                }
                lit.setInfecte(doitEtreInfecte);
                lit.setDateSynchro(LocalDateTime.now());
                litRepository.save(lit);
                modifies++;
            }
        }

        int inconnus = (int) infectes.stream().filter(id -> !connus.contains(id)).count();

        return new int[]{modifies, inconnus};
    }

    private Boolean booleen(String valeur) {
        if (valeur == null) {
            return null;
        }
        return switch (valeur.trim().toLowerCase()) {
            case "true", "1", "oui", "yes", "vrai" -> true;
            case "false", "0", "non", "no", "faux" -> false;
            default -> null;
        };
    }

    private String tronquer(String message) {
        if (message == null) {
            return "Erreur inconnue";
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    private static class DonneeInfection {
        private String idExterne;
        private Boolean infecte;
    }
}
