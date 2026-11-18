package tn.softsys.buanderie.service.referentiel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.exception.BusinessException;


@Slf4j
@Component
public class CubixClient {

    public static final String ENTETE_JETON = "X-Cubix-Token";
    private static final String REPONSE_OK = "-[OK]-";

    /** Réponse de Login/Mobile : le jeton et les codes de privilège (LIG…) qui suivent dans le tableau. */
    public record Connexion(String jeton, List<String> privileges) { }

    /** Réponse de Login/MobileBadge : jeton, login du compte, droits. */
    public record ConnexionBadge(String jeton, String login, boolean administrateur, List<String> privileges) { }

    public record Identite(Long id, String nom, boolean administrateur, String codeBarre) { }

    /** Une étape du process CUBIX : catégorie (0 collage … 7 livraison) et ids du personnel habilité. */
    public record Etape(Long id, String nom, int categorie, List<Long> personnelIds) { }

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${cubix.base-url:}")
    private String baseUrl;

    @Value("${cubix.chemin-login:/Login/Mobile}")
    private String cheminLogin;

    /** Clé partagée avec CUBIX (web.config : CleBadge) : seul ce serveur peut demander une connexion par badge. */
    @Value("${cubix.cle-badge:}")
    private String cleBadge;

    @Value("${cubix.chemin-api:/BuanderieApi}")
    private String cheminApi;

    public CubixClient(ObjectMapper objectMapper,
                       @Value("${cubix.delai-connexion-ms:5000}") int delaiConnexionMs,
                       @Value("${cubix.delai-lecture-ms:20000}") int delaiLectureMs) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory fabrique = new SimpleClientHttpRequestFactory();
        fabrique.setConnectTimeout(delaiConnexionMs);
        fabrique.setReadTimeout(delaiLectureMs);
        this.restTemplate = new RestTemplate(fabrique);
    }

    /** Se connecte à CUBIX et renvoie le jeton avec les privilèges. */
    public Connexion seConnecter(String login, String motDePasse) {
        String base = base();
        URI uri = UriComponentsBuilder.fromUriString(base + cheminLogin)
                .queryParam("a", "{a}").queryParam("b", "{b}").queryParam("v", "1")
                .encode().buildAndExpand(login, motDePasse).toUri();

        String corps;
        try {
            corps = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(entetes(null)), String.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw new BusinessException("Connexion à CUBIX impossible : HTTP " + e.getStatusCode().value()
                    + " sur " + cheminLogin + (e.getStatusCode().value() == 404 ? " (vérifiez cubix.base-url)." : "."));
        } catch (ResourceAccessException e) {
            throw new BusinessException("CUBIX injoignable : adresse erronée, serveur arrêté ou délai dépassé.");
        }

        JsonNode racine;
        try {
            racine = objectMapper.readTree(corps == null ? "" : corps);
        } catch (Exception e) {
            throw new BusinessException("Réponse de connexion CUBIX non reconnue : un tableau JSON est attendu.");
        }
        if (racine == null || !racine.isArray() || racine.size() == 0) {
            throw new BusinessException("Réponse de connexion CUBIX non reconnue : un tableau JSON est attendu.");
        }
        if (!REPONSE_OK.equals(racine.get(0).asText())) {
            throw new BusinessException("Login ou mot de passe incorrect.");
        }
        if (racine.size() < 2 || racine.get(1).asText().isBlank()) {
            throw new BusinessException("CUBIX n'a renvoyé aucun jeton.");
        }
        List<String> privileges = new ArrayList<>();
        for (int i = 2; i < racine.size(); i++) {
            String code = racine.get(i).asText().trim();
            if (code.startsWith("LIG") && !privileges.contains(code)) {
                privileges.add(code);
            }
        }
        return new Connexion(racine.get(1).asText().trim(), privileges);
    }

    /** Connexion par badge : CUBIX renvoie le même jeton qu'avec login et mot de passe, sans que le mot de passe circule. */
    public ConnexionBadge seConnecterParBadge(String codeBarre) {
        if (cleBadge == null || cleBadge.isBlank()) {
            throw new BusinessException("Connexion par badge non configurée : définissez cubix.cle-badge (CUBIX_CLE_BADGE).");
        }
        org.springframework.util.LinkedMultiValueMap<String, String> form = new org.springframework.util.LinkedMultiValueMap<>();
        form.add("c", codeBarre.trim());
        form.add("k", cleBadge);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setAccept(List.of(MediaType.ALL));

        String corps;
        try {
            corps = restTemplate.exchange(URI.create(base() + cheminLogin + "Badge"), HttpMethod.POST,
                    new HttpEntity<>(form, h), String.class).getBody();
        } catch (HttpStatusCodeException e) {
            int s = e.getStatusCode().value();
            if (s == 403) {
                throw new BusinessException("CUBIX a refusé la connexion par badge : clé (cubix.cle-badge) incorrecte.");
            }
            throw new BusinessException("Connexion par badge à CUBIX impossible : HTTP " + s
                    + (s == 404 ? " (Login/MobileBadge absent : mettez CUBIX à jour)." : "."));
        } catch (ResourceAccessException e) {
            throw new BusinessException("CUBIX injoignable : adresse erronée, serveur arrêté ou délai dépassé.");
        }

        JsonNode r;
        try {
            r = objectMapper.readTree(corps == null ? "" : corps);
        } catch (Exception e) {
            throw new BusinessException("Réponse de connexion par badge non reconnue : JSON attendu.");
        }
        if (!r.path("ok").asBoolean(false)) {
            throw new BusinessException(r.path("message").asText("Badge non reconnu"));
        }
        String jeton = r.path("jeton").asText("").trim();
        String login = r.path("login").asText("").trim();
        if (jeton.isEmpty() || login.isEmpty()) {
            throw new BusinessException("CUBIX n'a renvoyé ni jeton ni login pour ce badge.");
        }
        List<String> privileges = new ArrayList<>();
        for (JsonNode p : r.path("privileges")) {
            String code = p.asText().trim();
            if (code.startsWith("LIG") && !privileges.contains(code)) {
                privileges.add(code);
            }
        }
        return new ConnexionBadge(jeton, login, r.path("administrateur").asBoolean(false), privileges);
    }

    /** Vérifie le jeton et donne l'utilisateur (Ping). */
    public Identite ping(String jeton) {
        JsonNode racine = get("/Ping", jeton);
        JsonNode u = racine.path("utilisateur");
        return new Identite(u.path("id").isNumber() ? u.path("id").asLong() : null,
                u.path("nom").asText(null), u.path("administrateur").asBoolean(false),
                u.path("codeBarre").isNull() || u.path("codeBarre").isMissingNode() ? null : u.path("codeBarre").asText());
    }

    /** Étapes du process avec le personnel habilité. */
    public List<Etape> etapes(String jeton) {
        JsonNode items = get("/Etapes", jeton).path("items");
        List<Etape> etapes = new ArrayList<>();
        for (JsonNode e : items) {
            List<Long> ids = new ArrayList<>();
            e.path("personnelIds").forEach(n -> ids.add(n.asLong()));
            etapes.add(new Etape(e.path("id").asLong(), e.path("nom").asText(""), e.path("categorie").asInt(-1), ids));
        }
        return etapes;
    }

    // ------------------------------------------------------------------------------------------

    private JsonNode get(String action, String jeton) {
        if (jeton == null || jeton.isBlank()) {
            throw new BusinessException("Session CUBIX absente ou expirée (le serveur a peut-être redémarré) : déconnectez-vous puis reconnectez-vous.");
        }
        try {
            String corps = restTemplate.exchange(URI.create(base() + cheminApi + action), HttpMethod.GET,
                    new HttpEntity<>(entetes(jeton)), String.class).getBody();
            JsonNode racine = objectMapper.readTree(corps == null ? "" : corps);
            if (!racine.path("ok").asBoolean(false)) {
                throw new BusinessException("CUBIX a répondu sans succès sur " + action + ".");
            }
            return racine;
        } catch (HttpStatusCodeException e) {
            int s = e.getStatusCode().value();
            if (s == 401) {
                throw new BusinessException("Session CUBIX refusée (jeton invalide) : reconnectez-vous.");
            }
            if (s == 403) {
                throw new BusinessException("Compte non habilité pour la buanderie dans CUBIX "
                        + "(à ajouter au personnel d'au moins une étape : Buanderie > Étapes > Personnel).");
            }
            if (s == 404) {
                throw new BusinessException("CUBIX : " + cheminApi + action + " introuvable (HTTP 404) : vérifiez cubix.base-url.");
            }
            throw new BusinessException("CUBIX a répondu HTTP " + s + " sur " + action + ".");
        } catch (ResourceAccessException e) {
            throw new BusinessException("CUBIX injoignable : adresse erronée, serveur arrêté ou délai dépassé.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Réponse CUBIX non reconnue sur " + action
                    + " : JSON attendu (jeton expiré ? reconnectez-vous).");
        }
    }

    private String base() {
        String b = baseUrl == null ? "" : baseUrl.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        if (b.isEmpty()) {
            throw new BusinessException("cubix.base-url n'est pas configuré (variable CUBIX_BASE_URL).");
        }
        if (b.toLowerCase().startsWith("http://") && !b.contains("localhost") && !b.contains("127.0.0.1")) {
            log.warn("CUBIX est appelé en HTTP non chiffré : le mot de passe circule en clair. Utilisez HTTPS.");
        }
        return b;
    }

    private HttpHeaders entetes(String jeton) {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.ALL));
        if (jeton != null) {
            h.set(ENTETE_JETON, jeton);
        }
        return h;
    }
}
