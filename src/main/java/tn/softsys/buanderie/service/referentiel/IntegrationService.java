package tn.softsys.buanderie.service.referentiel;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.Referentiel.ConfigurationIntegrationRequest;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ConfigurationIntegrationRepository;
import tn.softsys.buanderie.repository.EndpointIntegrationRepository;
import tn.softsys.buanderie.repository.ServiceRepository;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final ConfigurationIntegrationRepository configurationRepository;
    private final EndpointIntegrationRepository endpointRepository;
    private final ServiceRepository serviceRepository;

    @Value("${buanderie.integration.mode:INTERNE}")
    private String modeParDefaut;

    @Value("${buanderie.integration.connecteur:STANDARD}")
    private String connecteurParDefaut;

    @Value("${buanderie.integration.url-base:${cubix.base-url:}}")
    private String urlBaseParDefaut;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialiser() {
        serviceRepository.activerServicesSansEtat();
        serviceRepository.definirSourceManquante(SourceDonnee.INTERNE);

        ConfigurationIntegration config = configurationRepository.findById(ConfigurationIntegration.ID_UNIQUE)
                .orElse(null);

        if (config == null) {
            config = configurationRepository.save(
                    ConfigurationIntegration.builder()
                            .id(ConfigurationIntegration.ID_UNIQUE)
                            .mode(parse(ModeSource.class, modeParDefaut, ModeSource.INTERNE))
                            .typeConnecteur(parse(TypeConnecteur.class, connecteurParDefaut, TypeConnecteur.STANDARD))
                            .urlBase(vide(urlBaseParDefaut))
                            .typeAuthentification(TypeAuthentification.AUCUNE)
                            .frequenceMinutes(60)
                            .synchroAutomatique(true)
                            .build());
        }

        boolean externe = config.getMode() == ModeSource.EXTERNE;
        TypeConnecteur type = config.getTypeConnecteur() == null ? TypeConnecteur.STANDARD : config.getTypeConnecteur();

        for (Referentiel referentiel : Referentiel.values()) {
            EndpointIntegration endpoint = endpointRepository.findById(referentiel).orElse(null);
            if (endpoint == null) {
                endpointRepository.save(
                        EndpointIntegration.builder()
                                .referentiel(referentiel)
                                .chemin(ModeleConnecteur.chemin(type, referentiel))
                                .cheminListe(ModeleConnecteur.cheminListe(type, referentiel))
                                .correspondanceChamps(ModeleConnecteur.correspondance(type, referentiel))
                                .actif(externe && referentiel.isSynchronisationDisponible())
                                .build());
            } else if (type != TypeConnecteur.PERSONNALISE
                    && (endpoint.getCorrespondanceChamps() == null || endpoint.getCorrespondanceChamps().isEmpty())) {
                endpoint.setCorrespondanceChamps(ModeleConnecteur.correspondance(type, referentiel));
                if (endpoint.getCheminListe() == null) {
                    endpoint.setCheminListe(ModeleConnecteur.cheminListe(type, referentiel));
                }
                if (endpoint.getChemin() == null) {
                    endpoint.setChemin(ModeleConnecteur.chemin(type, referentiel));
                }
                endpointRepository.save(endpoint);
            }
        }
    }

    @Transactional(readOnly = true)
    public ConfigurationIntegration getConfiguration() {
        return configurationRepository.findById(ConfigurationIntegration.ID_UNIQUE)
                .orElseGet(() -> ConfigurationIntegration.builder()
                        .id(ConfigurationIntegration.ID_UNIQUE)
                        .mode(ModeSource.INTERNE)
                        .build());
    }

    @Transactional(readOnly = true)
    public List<EndpointIntegration> getEndpoints() {
        return endpointRepository.findAll().stream()
                .sorted(java.util.Comparator.comparingInt(e -> e.getReferentiel().ordinal()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EndpointIntegration getEndpoint(Referentiel referentiel) {
        return endpointRepository.findById(referentiel)
                .orElseThrow(() -> new BusinessException("Aucune URL configurée pour " + referentiel + "."));
    }

    @Transactional(readOnly = true)
    public boolean estImporte(Referentiel referentiel) {
        if (getConfiguration().getMode() != ModeSource.EXTERNE) {
            return false;
        }
        return endpointRepository.findById(referentiel)
                .map(e -> Boolean.TRUE.equals(e.getActif()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public String urlComplete(Referentiel referentiel) {
        EndpointIntegration endpoint = getEndpoint(referentiel);

        String chemin = vide(endpoint.getChemin());
        if (chemin == null) {
            throw new BusinessException("Aucune URL configurée pour " + referentiel + ".");
        }
        if (chemin.startsWith("http://") || chemin.startsWith("https://")) {
            return chemin;
        }

        String base = vide(getConfiguration().getUrlBase());
        if (base == null) {
            throw new BusinessException("L'URL de base de l'intégration n'est pas configurée.");
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + (chemin.startsWith("/") ? chemin : "/" + chemin);
    }

    @Transactional
    public ConfigurationIntegration modifier(ConfigurationIntegrationRequest request) {
        ConfigurationIntegration config = configurationRepository.findById(ConfigurationIntegration.ID_UNIQUE)
                .orElseGet(() -> ConfigurationIntegration.builder().id(ConfigurationIntegration.ID_UNIQUE).build());

        config.setMode(request.getMode());
        if (request.getTypeConnecteur() != null) {
            config.setTypeConnecteur(request.getTypeConnecteur());
        }
        if (config.getTypeConnecteur() == null) {
            config.setTypeConnecteur(TypeConnecteur.STANDARD);
        }
        if (request.getUrlBase() != null) {
            config.setUrlBase(vide(request.getUrlBase()));
        }
        if (request.getTypeAuthentification() != null) {
            config.setTypeAuthentification(request.getTypeAuthentification());
        }
        if (request.getJeton() != null) {
            config.setJeton(vide(request.getJeton()));
        }
        if (request.getNomEnteteCle() != null) {
            config.setNomEnteteCle(vide(request.getNomEnteteCle()));
        }
        if (request.getUtilisateur() != null) {
            config.setUtilisateur(vide(request.getUtilisateur()));
        }
        if (request.getMotDePasse() != null) {
            config.setMotDePasse(vide(request.getMotDePasse()));
        }
        if (request.getFrequenceMinutes() != null) {
            config.setFrequenceMinutes(request.getFrequenceMinutes());
        }
        if (request.getSynchroAutomatique() != null) {
            config.setSynchroAutomatique(request.getSynchroAutomatique());
        }

        if (request.getEndpoints() != null) {
            for (ConfigurationIntegrationRequest.EndpointRequest e : request.getEndpoints()) {
                EndpointIntegration endpoint = endpointRepository.findById(e.getReferentiel())
                        .orElseGet(() -> EndpointIntegration.builder().referentiel(e.getReferentiel()).build());
                if (e.getChemin() != null) {
                    endpoint.setChemin(vide(e.getChemin()));
                }
                if (e.getCheminListe() != null) {
                    endpoint.setCheminListe(vide(e.getCheminListe()));
                }
                if (e.getCorrespondanceChamps() != null) {
                    LinkedHashMap<String, String> champs = new LinkedHashMap<>();
                    e.getCorrespondanceChamps().forEach((cle, valeur) -> {
                        if (cle != null && vide(valeur) != null) {
                            champs.put(cle.trim(), valeur.trim());
                        }
                    });
                    endpoint.setCorrespondanceChamps(champs);
                }
                if (e.getActif() != null) {
                    endpoint.setActif(e.getActif());
                }
                endpointRepository.save(endpoint);
            }
        }

        if (config.getMode() == ModeSource.EXTERNE) {
            verifierConfigurationExterne(config);
        }

        return configurationRepository.save(config);
    }

    @Transactional
    public List<EndpointIntegration> appliquerModele(TypeConnecteur type) {
        if (type == TypeConnecteur.PERSONNALISE) {
            throw new BusinessException("Le type PERSONNALISE n'a pas de modèle : renseignez les URL et les champs.");
        }

        ConfigurationIntegration config = configurationRepository.findById(ConfigurationIntegration.ID_UNIQUE)
                .orElseThrow(() -> new BusinessException("Configuration d'intégration introuvable."));
        config.setTypeConnecteur(type);
        configurationRepository.save(config);

        for (Referentiel referentiel : Referentiel.values()) {
            EndpointIntegration endpoint = endpointRepository.findById(referentiel)
                    .orElseGet(() -> EndpointIntegration.builder().referentiel(referentiel).actif(false).build());
            endpoint.setChemin(ModeleConnecteur.chemin(type, referentiel));
            endpoint.setCheminListe(ModeleConnecteur.cheminListe(type, referentiel));
            endpoint.setCorrespondanceChamps(ModeleConnecteur.correspondance(type, referentiel));
            endpointRepository.save(endpoint);
        }
        return getEndpoints();
    }

    @Transactional
    public void enregistrerResultat(Referentiel referentiel, String statut, String message, LocalDateTime date) {
        endpointRepository.findById(referentiel).ifPresent(e -> {
            e.setDerniereSynchro(date);
            e.setDernierStatut(statut);
            e.setDernierMessage(message != null && message.length() > 500 ? message.substring(0, 497) + "..." : message);
            endpointRepository.save(e);
        });
    }

    private void verifierConfigurationExterne(ConfigurationIntegration config) {
        List<EndpointIntegration> actifs = endpointRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActif()))
                .toList();

        if (actifs.isEmpty()) {
            throw new BusinessException("En mode externe, au moins une URL doit être active.");
        }

        for (EndpointIntegration e : actifs) {
            String chemin = vide(e.getChemin());
            if (chemin == null) {
                throw new BusinessException("L'URL de " + e.getReferentiel() + " est active mais vide.");
            }
            boolean complete = chemin.startsWith("http://") || chemin.startsWith("https://");
            if (!complete && vide(config.getUrlBase()) == null) {
                throw new BusinessException("L'URL de base est obligatoire pour " + e.getReferentiel() + ".");
            }
            if (e.getReferentiel().isSynchronisationDisponible()
                    && (e.getCorrespondanceChamps() == null || vide(e.getCorrespondanceChamps().get("idExterne")) == null)) {
                throw new BusinessException("Pour " + e.getReferentiel()
                        + ", indiquez au minimum le champ source de « idExterne ».");
            }
        }

        if (config.getTypeAuthentification() == TypeAuthentification.BASIC
                && (vide(config.getUtilisateur()) == null || vide(config.getMotDePasse()) == null)) {
            throw new BusinessException("L'authentification BASIC demande un utilisateur et un mot de passe.");
        }
        if ((config.getTypeAuthentification() == TypeAuthentification.BEARER
                || config.getTypeAuthentification() == TypeAuthentification.CLE_API)
                && vide(config.getJeton()) == null) {
            throw new BusinessException("Le jeton ou la clé API est obligatoire pour ce type d'authentification.");
        }
    }

    private <E extends Enum<E>> E parse(Class<E> type, String valeur, E defaut) {
        return Arrays.stream(type.getEnumConstants())
                .filter(m -> m.name().equalsIgnoreCase(valeur == null ? "" : valeur.trim()))
                .findFirst()
                .orElse(defaut);
    }

    private String vide(String valeur) {
        if (valeur == null) {
            return null;
        }
        String v = valeur.trim();
        return v.isEmpty() ? null : v;
    }
}