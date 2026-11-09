package tn.softsys.buanderie.service.referentiel;

import java.time.LocalDateTime;
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
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatTechniqueMachine;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.entity.enums.TypeMachine;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.repository.MachineRepository;

@Slf4j
@Service
public class SynchronisationMachinesService {

    private static final int TAILLE_PAGE = 200;

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final MachineRepository machineRepository;
    private final TypeMachineResolver typeMachineResolver;
    private final JournalSynchronisationRepository journalRepository;
    private final TransactionTemplate transactionTemplate;

    private final Object verrou = new Object();

    public SynchronisationMachinesService(
            IntegrationService integrationService,
            ConnecteurRest connecteur,
            MachineRepository machineRepository,
            TypeMachineResolver typeMachineResolver,
            JournalSynchronisationRepository journalRepository,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.machineRepository = machineRepository;
        this.typeMachineResolver = typeMachineResolver;
        this.journalRepository = journalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int tester() {
        if (!integrationService.estImporte(Referentiel.MACHINE)) {
            throw new BusinessException("Les machines ne sont pas importées : mode interne ou URL inactive.");
        }
        return recuperer(1).size();
    }

    public JournalSynchronisation synchroniser(String declencheur) {
        synchronized (verrou) {
            if (!integrationService.estImporte(Referentiel.MACHINE)) {
                throw new BusinessException("Les machines sont gérées en interne : rien à synchroniser.");
            }

            JournalSynchronisation journal = JournalSynchronisation.builder()
                    .referentiel(Referentiel.MACHINE)
                    .source(SourceDonnee.EXTERNE)
                    .dateDebut(LocalDateTime.now())
                    .statut("EN_COURS")
                    .declencheur(declencheur)
                    .build();

            try {
                List<DonneeMachine> recus = dedoublonner(recuperer(0));

                if (recus.isEmpty()) {
                    throw new BusinessException(
                            "La source n'a renvoyé aucune machine exploitable : synchronisation annulée.");
                }

                int[] c = transactionTemplate.execute(status -> appliquer(recus));

                journal.setNbRecus(recus.size());
                journal.setNbCrees(c[0]);
                journal.setNbMisAJour(c[1]);
                journal.setNbDesactives(0);
                journal.setStatut("SUCCES");

                StringBuilder message = new StringBuilder()
                        .append(recus.size()).append(" reçues, ")
                        .append(c[0]).append(" créées, ")
                        .append(c[1]).append(" mises à jour");
                if (c[2] > 0) {
                    message.append(", ").append(c[2]).append(" ignorées (code-barres déjà utilisé)");
                }
                if (c[4] > 0) {
                    message.append(", ").append(c[4])
                            .append(" créée(s) comme LAVEUR faute de type identifiable : à corriger");
                }
                if (c[5] > 0) {
                    message.append(", ").append(c[5]).append(" numéro(s) de série en doublon ignoré(s)");
                }
                if (c[3] > 0) {
                    message.append(", ").append(c[3]).append(" absentes de la source (conservées)");
                }
                message.append(".");
                journal.setMessage(message.toString());
            } catch (Exception e) {
                log.error("Synchronisation des machines échouée : {}", e.getMessage(), e);
                journal.setStatut("ECHEC");
                journal.setMessage(tronquer(e.getMessage()));
            }

            journal.setDateFin(LocalDateTime.now());
            JournalSynchronisation sauve = journalRepository.save(journal);
            integrationService.enregistrerResultat(
                    Referentiel.MACHINE, sauve.getStatut(), sauve.getMessage(), sauve.getDateFin());
            return sauve;
        }
    }

    private List<DonneeMachine> recuperer(int pagesMax) {
        EndpointIntegration endpoint = integrationService.getEndpoint(Referentiel.MACHINE);
        Map<String, String> champs = endpoint.getCorrespondanceChamps();

        if (champs == null || champs.get("idExterne") == null) {
            throw new BusinessException("La correspondance du champ « idExterne » des machines n'est pas configurée.");
        }

        ConfigurationIntegration config = integrationService.getConfiguration();
        String url = integrationService.urlComplete(Referentiel.MACHINE);

        List<JsonNode> elements = config.getTypeConnecteur() == TypeConnecteur.CUBIX
                ? connecteur.recupererListePaginee(url, config, endpoint.getCheminListe(), TAILLE_PAGE, pagesMax)
                : connecteur.recupererListe(url, config, endpoint.getCheminListe());

        return elements.stream()
                .map(e -> convertir(e, champs))
                .filter(d -> d.idExterne != null)
                .toList();
    }

    private DonneeMachine convertir(JsonNode element, Map<String, String> champs) {
        DonneeMachine d = new DonneeMachine();
        d.idExterne = connecteur.texte(element, champs.get("idExterne"));
        d.code = connecteur.texte(element, champs.get("code"));
        d.designation = connecteur.texte(element, champs.get("designation"));
        d.reference = connecteur.texte(element, champs.get("reference"));
        d.etatImmobilisation = etat(connecteur.texte(element, champs.get("etatActif")));
        d.type = typeMachineResolver.resoudre(connecteur.texte(element, champs.get("type")));
        d.marque = connecteur.texte(element, champs.get("marque"));
        d.capaciteKg = decimal(connecteur.texte(element, champs.get("capaciteKg")));
        d.localisation = connecteur.texte(element, champs.get("localisation"));
        return d;
    }

    private List<DonneeMachine> dedoublonner(List<DonneeMachine> recus) {
        Map<String, DonneeMachine> uniques = new LinkedHashMap<>();
        for (DonneeMachine d : recus) {
            uniques.putIfAbsent(d.idExterne, d);
        }
        return List.copyOf(uniques.values());
    }

    private int[] appliquer(List<DonneeMachine> recus) {
        Map<String, Machine> existants = machineRepository.findBySource(SourceDonnee.EXTERNE).stream()
                .filter(m -> m.getIdExterne() != null)
                .collect(Collectors.toMap(Machine::getIdExterne, Function.identity(), (a, b) -> a));

        Map<String, Long> codesPris = new HashMap<>();
        Map<String, Long> referencesPrises = new HashMap<>();
        for (Machine m : machineRepository.findAll()) {
            if (m.getCode() != null) {
                codesPris.putIfAbsent(m.getCode(), m.getId());
            }
            if (m.getReference() != null) {
                referencesPrises.putIfAbsent(m.getReference(), m.getId());
            }
        }

        int crees = 0;
        int misAJour = 0;
        int ignores = 0;
        int typesIndetermines = 0;
        int referencesIgnorees = 0;
        LocalDateTime maintenant = LocalDateTime.now();

        for (DonneeMachine d : recus) {
            Machine machine = existants.get(d.idExterne);

            String code = limiter(d.code, 100);
            String reference = limiter(d.reference, 30);

            if (machine == null) {
                if (code != null && codesPris.containsKey(code)) {
                    ignores++;
                    continue;
                }
                if (reference != null && referencesPrises.containsKey(reference)) {
                    reference = null;
                    referencesIgnorees++;
                }

                TypeMachine type = d.type != null ? d.type : typeMachineResolver.resoudre(d.designation);
                if (type == null) {
                    type = TypeMachine.LAVEUR;
                    typesIndetermines++;
                }

                Machine sauvee = machineRepository.save(Machine.builder()
                        .source(SourceDonnee.EXTERNE)
                        .idExterne(d.idExterne)
                        .code(code)
                        .reference(reference)
                        .designation(limiter(d.designation, 255))
                        .type(type)
                        .marque(limiter(d.marque, 100))
                        .capaciteKg(d.capaciteKg)
                        .localisation(limiter(d.localisation, 100))
                        .etatImmobilisation(d.etatImmobilisation)
                        .etatTechnique(etatTechniqueInitial(d.etatImmobilisation))
                        .dateSynchro(maintenant)
                        .build());

                if (code != null) {
                    codesPris.put(code, sauvee.getId());
                }
                if (reference != null) {
                    referencesPrises.put(reference, sauvee.getId());
                }
                crees++;
                continue;
            }

            boolean modifie = false;

            String designation = limiter(d.designation, 255);
            if (designation != null && !Objects.equals(machine.getDesignation(), designation)) {
                machine.setDesignation(designation);
                modifie = true;
            }
            if (d.type != null && machine.getType() != d.type) {
                machine.setType(d.type);
                modifie = true;
            }
            String marque = limiter(d.marque, 100);
            if (marque != null && !Objects.equals(machine.getMarque(), marque)) {
                machine.setMarque(marque);
                modifie = true;
            }
            if (d.capaciteKg != null && !Objects.equals(machine.getCapaciteKg(), d.capaciteKg)) {
                machine.setCapaciteKg(d.capaciteKg);
                modifie = true;
            }
            String localisation = limiter(d.localisation, 100);
            if (localisation != null && !Objects.equals(machine.getLocalisation(), localisation)) {
                machine.setLocalisation(localisation);
                modifie = true;
            }
            if (d.etatImmobilisation != null && machine.getEtatImmobilisation() != d.etatImmobilisation) {
                machine.setEtatImmobilisation(d.etatImmobilisation);
                if (estInactive(d.etatImmobilisation)
                        && machine.getEtatTechnique() != EtatTechniqueMachine.MISE_AU_REBUT) {
                    machine.setEtatTechnique(EtatTechniqueMachine.HORS_SERVICE);
                }
                modifie = true;
            }
            if (code != null && !Objects.equals(machine.getCode(), code)) {
                Long proprietaire = codesPris.get(code);
                if (proprietaire != null && !proprietaire.equals(machine.getId())) {
                    ignores++;
                } else {
                    machine.setCode(code);
                    codesPris.put(code, machine.getId());
                    modifie = true;
                }
            }
            if (reference != null && !Objects.equals(machine.getReference(), reference)) {
                Long proprietaire = referencesPrises.get(reference);
                if (proprietaire != null && !proprietaire.equals(machine.getId())) {
                    referencesIgnorees++;
                } else {
                    machine.setReference(reference);
                    referencesPrises.put(reference, machine.getId());
                    modifie = true;
                }
            }

            if (modifie) {
                machine.setDateSynchro(maintenant);
                machineRepository.save(machine);
                misAJour++;
            }
        }

        Set<String> idsRecus = recus.stream().map(d -> d.idExterne).collect(Collectors.toSet());
        int absents = (int) existants.keySet().stream().filter(id -> !idsRecus.contains(id)).count();

        return new int[]{crees, misAJour, ignores, absents, typesIndetermines, referencesIgnorees};
    }

    private EtatTechniqueMachine etatTechniqueInitial(EtatImmobilisation etat) {
        return estInactive(etat) ? EtatTechniqueMachine.HORS_SERVICE : EtatTechniqueMachine.FONCTIONNEL;
    }

    private boolean estInactive(EtatImmobilisation etat) {
        return etat == EtatImmobilisation.VENDU
                || etat == EtatImmobilisation.PERDU
                || etat == EtatImmobilisation.VOLE
                || etat == EtatImmobilisation.MISE_AU_REBUT
                || etat == EtatImmobilisation.ANNULE;
    }

    private EtatImmobilisation etat(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return switch (Integer.parseInt(valeur.trim())) {
                case 0 -> EtatImmobilisation.DISPONIBLE;
                case 4 -> EtatImmobilisation.EN_COURS;
                case 1 -> EtatImmobilisation.VENDU;
                case 2 -> EtatImmobilisation.PERDU;
                case 5 -> EtatImmobilisation.MISE_AU_REBUT;
                case 6 -> EtatImmobilisation.VOLE;
                case 10 -> EtatImmobilisation.ANNULE;
                default -> null;
            };
        } catch (NumberFormatException e) {
            try {
                return EtatImmobilisation.valueOf(valeur.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    private Double decimal(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return Double.parseDouble(valeur.trim().replace(',', '.'));
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

    private static class DonneeMachine {
        private String idExterne;
        private String code;
        private String designation;
        private String reference;
        private EtatImmobilisation etatImmobilisation;
        private TypeMachine type;
        private String marque;
        private Double capaciteKg;
        private String localisation;
    }
}
