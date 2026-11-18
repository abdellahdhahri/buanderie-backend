package tn.softsys.buanderie.service.referentiel;

import tn.softsys.buanderie.service.management.OperationsEtape;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.EnvoiCubix;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.StatutEnvoi;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.EnvoiCubixRepository;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.security.CubixTokenStore;

/**
 * Reporte dans CUBIX (ValiderEtape) le tri, le pliage, le stockage et la livraison d'un linge importé de CUBIX.
 * Non bloquant : l'opération locale est déjà validée ; en cas d'échec, l'envoi est mis en attente (table envois_cubix)
 * et peut être renvoyé. Le lavage et le séchage ne passent pas ici (ils sont bloquants, voir CubixMachineEtatService).
 */
@Slf4j
@Service
public class CubixEtapeSyncService {

    private static final String MOTIF = "Synchronisation Buanderie";
    /** Catégories d'étape CUBIX : 0 collage, 1 réception, 2 tri, 5 pliage, 6 stockage, 7 livraison. */
    private static final Map<TypeOperation, Integer> CATEGORIES = Map.of(
            TypeOperation.COLLECTE, 0,
            TypeOperation.DECLARATION_SAC_INFECTE, 0,
            TypeOperation.RECEPTION_BAC, 1,
            TypeOperation.RECEPTION_SAC_INFECTE, 1,
            TypeOperation.TRI, 2,
            TypeOperation.PLIAGE, 5,
            TypeOperation.MISE_EN_STOCKAGE, 6,
            TypeOperation.LIVRAISON, 7);

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final CubixMachineImportService importService;
    private final CubixClient cubixClient;
    private final CubixTokenStore cubixTokens;
    private final EnvoiCubixRepository envoiRepository;
    private final ChambreRepository chambreRepository;
    private final PersonnelRepository personnelRepository;
    private final TransactionTemplate transactionNouvelle;

    public CubixEtapeSyncService(IntegrationService integrationService,
                                 ConnecteurRest connecteur,
                                 CubixMachineImportService importService,
                                 CubixClient cubixClient,
                                 CubixTokenStore cubixTokens,
                                 EnvoiCubixRepository envoiRepository,
                                 ChambreRepository chambreRepository,
                                 PersonnelRepository personnelRepository,
                                 @Qualifier("postgresTransactionManager") PlatformTransactionManager transactionManager) {
        this.integrationService = integrationService;
        this.connecteur = connecteur;
        this.importService = importService;
        this.cubixClient = cubixClient;
        this.cubixTokens = cubixTokens;
        this.envoiRepository = envoiRepository;
        this.chambreRepository = chambreRepository;
        this.personnelRepository = personnelRepository;
        this.transactionNouvelle = new TransactionTemplate(transactionManager);
        this.transactionNouvelle.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Appelé à l'écriture d'une ligne d'historique : prépare l'envoi (sans requête de lecture lourde) puis le
     * déclenche après la validation de la transaction locale. Ne lève jamais d'exception.
     */
    public void planifier(HistoriqueLinge h) {
        try {
            Integer categorie = h == null ? null : CATEGORIES.get(h.getTypeOperation());
            Linge linge = h == null ? null : h.getLinge();
            if (categorie == null || linge == null || linge.getSource() != SourceDonnee.EXTERNE
                    || linge.getIdExterne() == null) {
                return;
            }

            EnvoiCubix envoi = EnvoiCubix.builder()
                    .lingeId(linge.getId())
                    .lingeIdExterne(linge.getIdExterne())
                    .typeOperation(h.getTypeOperation())
                    .categorie(categorie)
                    .statut(StatutEnvoi.EN_ATTENTE)
                    .dateCreation(LocalDateTime.now())
                    .build();

            if (h.getPersonnelId() != null) {
                Personnel agent = personnelRepository.findById(h.getPersonnelId()).orElse(null);
                if (agent != null && agent.getSource() == SourceDonnee.EXTERNE) {
                    envoi.setPersonnelIdExterne(agent.getIdExterne());
                }
            }

            if (h.getTypeOperation() == TypeOperation.COLLECTE) {
                envoi.setBacIdExterne("0");    // collage : 0 = blanc
            }
            if (h.getTypeOperation() == TypeOperation.DECLARATION_SAC_INFECTE) {
                envoi.setBacIdExterne("-1");   // collage : -1 = infecté
            }

            if (h.getTypeOperation() == TypeOperation.TRI) {
                if (linge.getBac() != null && linge.getBac().getSourceDonnee() == SourceDonnee.EXTERNE
                        && linge.getBac().getIdExterne() != null) {
                    envoi.setBacIdExterne(linge.getBac().getIdExterne());
                } else {
                    log.info("Tri non envoyé à CUBIX (linge {}) : le bac de tri n'est pas un bac importé de CUBIX.",
                            linge.getIdExterne());
                    return;
                }
            }

            if (h.getTypeOperation() == TypeOperation.LIVRAISON) {
                if (linge.getChambreId() != null) {
                    Chambre chambre = chambreRepository.findById(linge.getChambreId()).orElse(null);
                    if (chambre != null) {
                        envoi.setChambreCodeBarre(chambre.getCodeBarre());
                    }
                }
                if (envoi.getChambreCodeBarre() == null) {
                    log.info("Livraison non envoyée à CUBIX (linge {}) : aucune chambre (code-barres) connue.",
                            linge.getIdExterne());
                    return;
                }
            }

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        traiter(envoi);
                    }
                });
            } else {
                traiter(envoi);
            }
        } catch (Exception e) {
            log.warn("Préparation de l'envoi CUBIX ignorée : {}", e.getMessage());
        }
    }

    private void traiter(EnvoiCubix envoi) {
        try {
            if (!actif()) {
                return;
            }
            envoyer(envoi);
        } catch (Exception e) {
            log.warn("Envoi CUBIX en échec ({} linge {}) : {}", envoi.getTypeOperation(), envoi.getLingeIdExterne(),
                    e.getMessage());
            envoi.setTentatives(1);
            envoi.setErreur(tronquer(e.getMessage()));
            envoi.setDateDernierEssai(LocalDateTime.now());
            try {
                transactionNouvelle.executeWithoutResult(s -> envoiRepository.save(envoi));
            } catch (Exception ex) {
                log.error("Impossible de mettre l'envoi CUBIX en attente : {}", ex.getMessage());
            }
        }
    }

    /** Renvoie tous les envois en attente avec le jeton de l'utilisateur connecté. */
    public Map<String, Object> renvoyerTout() {
        exigerActif();
        int ok = 0;
        int ko = 0;
        for (EnvoiCubix e : envoiRepository.findByStatutOrderByDateCreationAsc(StatutEnvoi.EN_ATTENTE)) {
            if (rejouer(e)) {
                ok++;
            } else {
                ko++;
            }
        }
        return Map.of("envoyes", ok, "echecs", ko);
    }

    public EnvoiCubix renvoyer(Long id) {
        exigerActif();
        EnvoiCubix e = envoiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Envoi introuvable : " + id));
        if (e.getStatut() == StatutEnvoi.EN_ATTENTE) {
            rejouer(e);
        }
        return e;
    }

    public EnvoiCubix abandonner(Long id) {
        EnvoiCubix e = envoiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Envoi introuvable : " + id));
        e.setStatut(StatutEnvoi.ABANDONNE);
        return envoiRepository.save(e);
    }

    public List<EnvoiCubix> lister(boolean tous) {
        return tous
                ? envoiRepository.findAll()
                : envoiRepository.findByStatutOrderByDateCreationAsc(StatutEnvoi.EN_ATTENTE);
    }

    private boolean rejouer(EnvoiCubix e) {
        e.setTentatives(e.getTentatives() + 1);
        e.setDateDernierEssai(LocalDateTime.now());
        try {
            envoyer(e);
            e.setStatut(StatutEnvoi.ENVOYE);
            e.setErreur(null);
            envoiRepository.save(e);
            return true;
        } catch (Exception ex) {
            e.setErreur(tronquer(ex.getMessage()));
            envoiRepository.save(e);
            return false;
        }
    }

    private void envoyer(EnvoiCubix e) {
        var config = integrationService.getConfiguration();
        Long etapeId = cubixClient.etapes(cubixTokens.jetonCourant()).stream()
                .filter(x -> x.categorie() == e.getCategorie())
                .map(CubixClient.Etape::id)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "CUBIX : aucune étape de catégorie " + e.getCategorie() + " n'est configurée."));

        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        champs.add("etapeId", String.valueOf(etapeId));
        champs.add("forcer", "true");
        champs.add("motif", MOTIF);
        champs.add("controlerHabilitation", "false");
        if (e.getPersonnelIdExterne() != null) {
            champs.add("personnelId", e.getPersonnelIdExterne());
        }
        champs.add("lignes[0].LingeId", e.getLingeIdExterne());
        if (e.getBacIdExterne() != null) {
            champs.add("lignes[0].BacId", e.getBacIdExterne());
        }
        if (e.getChambreCodeBarre() != null) {
            champs.add("lignes[0].ChambreCodeBarre", e.getChambreCodeBarre());
        }
        connecteur.ecrireFormulaire(importService.baseApi() + "/ValiderEtape", config, champs);
    }

    private boolean actif() {
        var config = integrationService.getConfiguration();
        return config.getMode() == ModeSource.EXTERNE
                && config.getTypeConnecteur() == TypeConnecteur.CUBIX
                && config.getTypeAuthentification() == TypeAuthentification.CUBIX;
    }

    private void exigerActif() {
        if (!actif()) {
            throw new BusinessException(
                    "Le renvoi vers CUBIX n'est disponible qu'en mode externe, connecteur CUBIX, authentification CUBIX.");
        }
    }

    private String tronquerTexte(String texte, int max) {
        if (texte == null) {
            return null;
        }
        return texte.length() > max ? texte.substring(0, max) : texte;
    }

    private String tronquer(String message) {
        if (message == null) {
            return "Erreur inconnue";
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }
}
