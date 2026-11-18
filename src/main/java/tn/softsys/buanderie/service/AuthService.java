package tn.softsys.buanderie.service;

import tn.softsys.buanderie.service.referentiel.CubixClient;

import tn.softsys.buanderie.security.CubixTokenStore;

import tn.softsys.buanderie.exception.BusinessException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tn.softsys.buanderie.dto.LoginResponseDTO;
import tn.softsys.buanderie.dto.UserDTO;
import tn.softsys.buanderie.security.JwtService;
import tn.softsys.buanderie.security.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    @Value("${cubix.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private final JwtService jwtService;
    private final CubixClient cubixClient;
    private final CubixTokenStore cubixTokens;

    public AuthService(JwtService jwtService, CubixClient cubixClient, CubixTokenStore cubixTokens) {
        this.jwtService = jwtService;
        this.cubixClient = cubixClient;
        this.cubixTokens = cubixTokens;
    }



    /**
     * Connexion avec un compte CUBIX : Login/Mobile donne un jeton, gardé côté serveur pour cet utilisateur
     * (il sert ensuite aux appels d'intégration). L'identité vient de Ping, les droits des étapes où la
     * personne est habilitée (Etapes). Le navigateur ne reçoit que le JWT de cette application.
     */
    public LoginResponseDTO login(String login, String password) {

        if (login == null || login.isBlank()) {
            throw new BusinessException("Login obligatoire");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("Mot de passe obligatoire");
        }

        String identifiant = login.trim();
        CubixClient.Connexion connexion = cubixClient.seConnecter(identifiant, password);
        String jeton = connexion.jeton();

        CubixClient.Identite identite = cubixClient.ping(jeton);
        List<CubixClient.Etape> etapes = cubixClient.etapes(jeton);

        boolean admin = identite.administrateur();
        List<CubixClient.Etape> habilitees = etapesHabilitees(admin, identite.id(), etapes);
        List<String> privileges = connexion.privileges();
        List<Map<String, Object>> etapesDto = new ArrayList<>();
        for (CubixClient.Etape e : habilitees) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", e.id());
            m.put("nom", e.nom());
            m.put("categorie", e.categorie());
            etapesDto.add(m);
        }

        cubixTokens.enregistrer(identifiant, jeton);

        UserDTO userDTO = new UserDTO(identite.id(), identifiant, identite.nom(), identite.codeBarre(), null, null);
        SecurityUser securityUser = new SecurityUser(identifiant, admin, privileges);
        String token = jwtService.generateToken(securityUser);

        LoginResponseDTO reponse = new LoginResponseDTO(token, identifiant, admin, privileges, identite.codeBarre(), userDTO);
        reponse.setEtapes(etapesDto);
        return reponse;
    }

    public void logout(String login) {
        cubixTokens.retirer(login);
    }

    /** Étapes où la personne est habilitée (PersonnelLinge_Etape) ; un administrateur les a toutes. */
    private List<CubixClient.Etape> etapesHabilitees(boolean admin, Long personnelId, List<CubixClient.Etape> etapes) {
        List<CubixClient.Etape> res = new ArrayList<>();
        for (CubixClient.Etape e : etapes) {
            if (admin || (personnelId != null && e.personnelIds().contains(personnelId))) {
                res.add(e);
            }
        }
        return res;
    }

    //login par badge

    public LoginResponseDTO loginByBadge(String codeBarre) {

        if (codeBarre == null || codeBarre.isBlank()) {
            throw new RuntimeException("Code barre obligatoire");
        }

        String loginUrl =
                baseUrl + "/Login/LoginByBadge";

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("codeBarre", codeBarre);

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form, headers);

        ResponseEntity<Map> response;

        try {

            response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de l'authentification par badge ASP.NET",
                    e
            );
        }

        Map data = response.getBody();

        if (data == null) {
            throw new RuntimeException(
                    "Réponse ASP.NET vide"
            );
        }

        Boolean success = (Boolean) data.get("success");

        if (!Boolean.TRUE.equals(success)) {

            String message =
                    data.get("message") != null
                            ? data.get("message").toString()
                            : "Badge non reconnu";

            throw new RuntimeException(message);
        }


        Map user =
                (Map) data.get("user");

        if (user == null) {
            throw new RuntimeException(
                    "Utilisateur introuvable"
            );
        }

        String login =
                user.get("login") != null
                        ? user.get("login").toString()
                        : null;

        boolean admin =
                Boolean.TRUE.equals(
                        user.get("administrateur")
                );


        List<String> privileges =
                new ArrayList<>();

        Object privilegesObject =
                data.get("privileges");

        if (privilegesObject instanceof List) {

            for (Object privilege : (List<?>) privilegesObject) {

                if (privilege != null) {

                    String code =
                            privilege.toString().trim();

                    if (!code.isEmpty() &&
                            !privileges.contains(code)) {

                        privileges.add(code);
                    }
                }
            }
        }

        Long userId = null;

        if (user.get("id") != null) {
            userId = ((Number) user.get("id")).longValue();
        }



        String nomPrenom =
                user.get("nomPrenom") != null
                        ? user.get("nomPrenom").toString()
                        : null;

        String codeBarreUser =
                user.get("codeBarre") != null
                        ? user.get("codeBarre").toString()
                        : null;



        boolean hasBuanderieAccess =
                admin ||
                        privileges.contains("LIG0");

        if (!hasBuanderieAccess) {

            throw new RuntimeException(
                    "Accès refusé au module Buanderie"
            );
        }
        UserDTO userDTO =
                new UserDTO(
                        userId,
                        login,
                        nomPrenom,
                        codeBarreUser,
                        null,
                        null

                );

        SecurityUser securityUser =
                new SecurityUser(
                        login,
                        admin,
                        privileges
                );


        String token =
                jwtService.generateToken(securityUser);
        return new LoginResponseDTO(
                token,
                login,
                admin,
                privileges,
                codeBarreUser,
                userDTO
        );
    }





    //retourner l utilisateur connectée
    private UserDTO getCurrentUser(String sessionId) {

        String currentUserUrl =
                baseUrl + "/Login/CurrentUser";

        HttpHeaders headers = new HttpHeaders();

        headers.add(
                HttpHeaders.COOKIE,
                "ASP.NET_SessionId=" + sessionId
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<Map> response;

        try {
            response = restTemplate.exchange(
                    currentUserUrl,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Impossible de récupérer l'utilisateur connecté ASP.NET",
                    e
            );
        }

        Map data = response.getBody();

        if (data == null) {
            throw new RuntimeException(
                    "Réponse CurrentUser ASP.NET vide"
            );
        }

        Long id = null;

        if (data.get("id") != null) {
            id = ((Number) data.get("id")).longValue();
        }

        String login =
                data.get("login") != null
                        ? data.get("login").toString()
                        : null;

        String nomPrenom =
                data.get("nomPrenom") != null
                        ? data.get("nomPrenom").toString()
                        : null;

        String codeBarre =
                data.get("codeBarre") != null
                        ? data.get("codeBarre").toString()
                        : null;

        Long serviceId = null;

        if (data.get("serviceId") != null) {
            serviceId =
                    ((Number) data.get("serviceId")).longValue();
        }

        return new UserDTO(
                id,
                login,
                nomPrenom,
                codeBarre,
                serviceId,
                null
        );
    }
}