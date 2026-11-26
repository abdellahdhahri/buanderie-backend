package tn.softsys.buanderie.service.referentiel;

import org.springframework.util.MultiValueMap;

import org.springframework.web.client.HttpStatusCodeException;

import tn.softsys.buanderie.security.CubixTokenStore;

import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class ConnecteurRest {

    private static final int PAGES_MAX_SECURITE = 1000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CubixTokenStore cubixTokens;

    public List<JsonNode> recupererListe(String url, ConfigurationIntegration config, String cheminListe) {
        String corps = appeler(url, config);
        if (corps == null || corps.isBlank()) {
            return List.of();
        }

        JsonNode racine;
        try {
            racine = objectMapper.readTree(corps);
        } catch (Exception e) {
            throw new BusinessException("La réponse de " + url + " n'est pas un JSON valide.");
        }

        JsonNode liste = naviguer(racine, cheminListe);
        if (liste == null || liste.isNull()) {
            throw new BusinessException("Liste introuvable dans la réponse (chemin : "
                    + (cheminListe == null || cheminListe.isBlank() ? "racine" : cheminListe) + ").");
        }
        if (!liste.isArray()) {
            throw new BusinessException("L'élément « "
                    + (cheminListe == null || cheminListe.isBlank() ? "racine" : cheminListe)
                    + " » de la réponse n'est pas une liste.");
        }

        List<JsonNode> elements = new ArrayList<>();
        liste.forEach(elements::add);
        return elements;
    }

    public List<JsonNode> recupererListePaginee(String url, ConfigurationIntegration config,
                                                String cheminListe, int taillePage, int pagesMax) {
        List<JsonNode> tous = new ArrayList<>();
        String signaturePrecedente = null;

        for (int page = 1; page <= PAGES_MAX_SECURITE; page++) {
            String urlPage = UriComponentsBuilder.fromUriString(url)
                    .replaceQueryParam("page", page)
                    .replaceQueryParam("pageSize", taillePage)
                    .build()
                    .toUriString();

            JsonNode racine = lireRacine(urlPage, config);

            JsonNode succes = naviguer(racine, "Success");
            if (succes != null && succes.isBoolean() && !succes.asBoolean()) {
                JsonNode message = naviguer(racine, "Message");
                throw new BusinessException("La source a renvoyé une erreur : "
                        + (message == null || message.isNull() ? "aucun détail" : message.asText()));
            }

            List<JsonNode> elements = extraireListe(racine, cheminListe, urlPage);

            String signature = elements.isEmpty() ? null : elements.get(0).toString();
            if (signature != null && signature.equals(signaturePrecedente)) {
                break;
            }
            signaturePrecedente = signature;

            tous.addAll(elements);

            if (elements.isEmpty() || (pagesMax > 0 && page >= pagesMax)) {
                break;
            }

            // Nouvelle API CUBIX (BuanderieApi) : {ok, total, page, pageSize, items}
            JsonNode ok = naviguer(racine, "ok");
            if (ok != null && ok.isBoolean()) {
                JsonNode totalApi = naviguer(racine, "total");
                if (totalApi != null && totalApi.canConvertToInt() && tous.size() >= totalApi.asInt()) {
                    break;
                }
                if (elements.size() < taillePage) {
                    break;
                }
                continue;
            }

            JsonNode suivante = naviguer(racine, "HasNextPage");
            if (suivante != null && suivante.isBoolean()) {
                if (!suivante.asBoolean()) {
                    break;
                }
                continue;
            }

            JsonNode total = naviguer(racine, "Total");
            if (total != null && total.canConvertToInt()) {
                if (tous.size() >= total.asInt()) {
                    break;
                }
                continue;
            }

            if (elements.size() < taillePage) {
                break;
            }
        }

        return tous;
    }

    /**
     * Appel GET. Avec la connexion CUBIX, un jeton refusé (401) est redemandé une fois, puis l'appel est rejoué.
     */
    private String appeler(String url, ConfigurationIntegration config) {
        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(entetes(config, url)), String.class).getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            if (config.getTypeAuthentification() != TypeAuthentification.CUBIX) {
                throw e;
            }
            cubixTokens.retirer(cubixTokens.loginCourant());
            throw new BusinessException("Session CUBIX expirée ou refusée : reconnectez-vous avec votre compte CUBIX.");
        } catch (HttpStatusCodeException e) {
            if (config.getTypeAuthentification() != TypeAuthentification.CUBIX) {
                throw e;
            }
            throw erreurCubix(e);
        }
    }

    /** Lecture GET d'une réponse JSON complète (utile pour les listes paginées gardées telles quelles). */
    public JsonNode lireJson(String url, ConfigurationIntegration config) {
        return lireRacine(url, config);
    }

    /** Écriture POST JSON (ex. ImporterMachines) avec les mêmes en-têtes d'authentification que les lectures. */
    public JsonNode ecrireJson(String url, ConfigurationIntegration config, Object corps) {
        return envoyer(url, config, corps, MediaType.APPLICATION_JSON);
    }

    /** Écriture POST en formulaire (clé=valeur, une clé répétée pour une liste) : le plus fiable avec ASP.NET MVC. */
    public JsonNode ecrireFormulaire(String url, ConfigurationIntegration config, MultiValueMap<String, String> champs) {
        return envoyer(url, config, champs, MediaType.APPLICATION_FORM_URLENCODED);
    }

    /**
     * Écriture en formulaire SANS jeton : pour ValiderEtape, ouvert comme la stérilisation (l'agent est identifié par
     * son badge côté CUBIX, qui contrôle son habilitation à l'étape).
     */
    public JsonNode ecrireFormulaireSansJeton(String url, MultiValueMap<String, String> champs) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            String reponse = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(champs, headers), String.class).getBody();
            try {
                return reponse == null || reponse.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(reponse);
            } catch (Exception e) {
                throw new BusinessException("La réponse de " + url + " n'est pas un JSON valide. Début reçu : " + extrait(reponse));
            }
        } catch (HttpStatusCodeException e) {
            throw erreurCubix(e);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Appel de " + url + " impossible : " + e.getMessage());
        }
    }

    /** Début d'une réponse non JSON (titre de la page HTML si présent), pour le diagnostic. */
    private String extrait(String corps) {
        if (corps == null) {
            return "(vide)";
        }
        java.util.regex.Matcher t = java.util.regex.Pattern
                .compile("<title>(.*?)</title>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
                .matcher(corps);
        String texte = t.find() ? "page « " + t.group(1).trim() + " »" : corps.replaceAll("\\s+", " ").trim();
        return texte.length() > 200 ? texte.substring(0, 200) + "…" : texte;
    }

    private JsonNode envoyer(String url, ConfigurationIntegration config, Object corps, MediaType typeContenu) {
        HttpHeaders headers = entetes(config, url);
        headers.setContentType(typeContenu);
        try {
            String reponse = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(corps, headers), String.class).getBody();
            return reponse == null || reponse.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(reponse);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (config.getTypeAuthentification() == TypeAuthentification.CUBIX) {
                cubixTokens.retirer(cubixTokens.loginCourant());
                throw new BusinessException("Session CUBIX expirée ou refusée : reconnectez-vous avec votre compte CUBIX.");
            }
            throw e;
        } catch (HttpStatusCodeException e) {
            throw erreurCubix(e);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("La réponse de " + url + " n'est pas un JSON valide.");
        }
    }

    /** Message lisible à partir d'une erreur CUBIX {"ok":false,"code":"...","message":"..."}. */
    private BusinessException erreurCubix(HttpStatusCodeException e) {
        String message = null;
        String corps = e.getResponseBodyAsString();
        try {
            JsonNode racine = objectMapper.readTree(corps);
            JsonNode m = racine.get("message");
            JsonNode code = racine.get("code");
            if (m != null && !m.isNull()) {
                message = m.asText() + (code != null && !code.isNull() ? " [" + code.asText() + "]" : "");
            }
        } catch (Exception ignore) {
            // corps non JSON (page d'erreur HTML d'ASP.NET) : on en extrait le titre ou le début
        }
        if ((message == null || message.isBlank()) && corps != null && !corps.isBlank()) {
            java.util.regex.Matcher t = java.util.regex.Pattern
                    .compile("<title>(.*?)</title>", java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(corps);
            String brut = t.find() ? t.group(1) : corps.replaceAll("<[^>]*>", " ");
            brut = brut.replaceAll("\\s+", " ").trim();
            if (!brut.isEmpty()) {
                message = "HTTP " + e.getStatusCode().value() + " - " + (brut.length() > 300 ? brut.substring(0, 300) : brut);
            }
        }
        if (message == null || message.isBlank()) {
            message = "CUBIX a répondu HTTP " + e.getStatusCode().value() + ".";
        }
        return new BusinessException("CUBIX : " + message);
    }

    private JsonNode lireRacine(String url, ConfigurationIntegration config) {
        String corps = appeler(url, config);
        if (corps == null || corps.isBlank()) {
            return objectMapper.createArrayNode();
        }

        try {
            return objectMapper.readTree(corps);
        } catch (Exception e) {
            throw new BusinessException("La réponse de " + url + " n'est pas un JSON valide.");
        }
    }

    private List<JsonNode> extraireListe(JsonNode racine, String cheminListe, String url) {
        JsonNode liste = naviguer(racine, cheminListe);
        if (liste == null || liste.isNull()) {
            throw new BusinessException("Liste introuvable dans la réponse (chemin : "
                    + (cheminListe == null || cheminListe.isBlank() ? "racine" : cheminListe) + ").");
        }
        if (!liste.isArray()) {
            throw new BusinessException("L'élément « "
                    + (cheminListe == null || cheminListe.isBlank() ? "racine" : cheminListe)
                    + " » de la réponse n'est pas une liste.");
        }
        List<JsonNode> elements = new ArrayList<>();
        liste.forEach(elements::add);
        return elements;
    }

    public String texte(JsonNode element, String chemin) {
        if (chemin == null || chemin.isBlank()) {
            return null;
        }
        JsonNode valeur = naviguer(element, chemin);
        if (valeur == null || valeur.isNull() || valeur.isMissingNode()) {
            return null;
        }
        String texte = valeur.isValueNode() ? valeur.asText() : valeur.toString();
        return texte == null || texte.isBlank() ? null : texte.trim();
    }

    private JsonNode naviguer(JsonNode noeud, String chemin) {
        if (chemin == null || chemin.isBlank()) {
            return noeud;
        }
        JsonNode courant = noeud;
        for (String partie : chemin.trim().split("\\.")) {
            if (courant == null) {
                return null;
            }
            courant = courant.get(partie);
        }
        return courant;
    }

    private HttpHeaders entetes(ConfigurationIntegration config, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        TypeAuthentification type = config.getTypeAuthentification() == null
                ? TypeAuthentification.AUCUNE
                : config.getTypeAuthentification();

        switch (type) {
            case BEARER -> {
                if (config.getJeton() != null && !config.getJeton().isBlank()) {
                    headers.setBearerAuth(config.getJeton().trim());
                }
            }
            case CLE_API -> {
                String nom = config.getNomEnteteCle() == null || config.getNomEnteteCle().isBlank()
                        ? "X-API-KEY"
                        : config.getNomEnteteCle().trim();
                if (config.getJeton() != null && !config.getJeton().isBlank()) {
                    headers.set(nom, config.getJeton().trim());
                }
            }
            case BASIC -> {
                if (config.getUtilisateur() != null && config.getMotDePasse() != null) {
                    headers.setBasicAuth(config.getUtilisateur(), config.getMotDePasse());
                }
            }
            case CUBIX -> {
                // Seule BuanderieApi exige le jeton ; les lectures /Importer/ (services, chambres, lits,
                // lits infectés, personnel) sont ouvertes dans CUBIX et se synchronisent sans session.
                if (url == null || !url.contains("/BuanderieApi/")) {
                    break;
                }
                String jeton = cubixTokens.jetonCourant();
                if (jeton == null) {
                    throw new BusinessException("Aucune session CUBIX : connectez-vous avec votre compte CUBIX "
                            + "(la synchronisation utilise le jeton de l'utilisateur connecté).");
                }
                headers.set(CubixClient.ENTETE_JETON, jeton);
            }
            case AUCUNE -> {
            }
        }
        return headers;
    }
}