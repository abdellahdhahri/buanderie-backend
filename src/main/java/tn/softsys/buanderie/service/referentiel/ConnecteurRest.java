package tn.softsys.buanderie.service.referentiel;

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

    public List<JsonNode> recupererListe(String url, ConfigurationIntegration config, String cheminListe) {
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(entetes(config)),
                String.class
        );

        String corps = response.getBody();
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

    private JsonNode lireRacine(String url, ConfigurationIntegration config) {
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(entetes(config)),
                String.class
        );

        String corps = response.getBody();
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

    private HttpHeaders entetes(ConfigurationIntegration config) {
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
            case AUCUNE -> {
            }
        }
        return headers;
    }
}