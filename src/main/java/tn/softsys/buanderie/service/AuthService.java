package tn.softsys.buanderie.service;

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


    public AuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }



    public LoginResponseDTO login(String login, String password) {


        if (login == null || login.isBlank()) {
            throw new RuntimeException("Login obligatoire");
        }

        if (password == null || password.isBlank()) {
            throw new RuntimeException("Mot de passe obligatoire");
        }

        String loginUrl = baseUrl + "/Login/CheckUser";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("Login", login);
        form.add("Password", password);
        form.add("touch", "false");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

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
                    "Erreur lors de l'authentification ASP.NET",
                    e
            );
        }

        Map data = response.getBody();

        if (data == null) {
            throw new RuntimeException("Réponse ASP.NET vide");
        }

        Boolean success = (Boolean) data.get("success");

        if (!Boolean.TRUE.equals(success)) {
            throw new RuntimeException("Login ou mot de passe incorrect");
        }

        Long userId = null;

        if (data.get("idUser") != null) {
            userId = ((Number) data.get("idUser")).longValue();
        }

        String username = data.get("username") != null
                ? data.get("username").toString()
                : null;

        String nom = data.get("nom") != null
                ? data.get("nom").toString()
                : "";

        String prenom = data.get("prenom") != null
                ? data.get("prenom").toString()
                : "";

        String nomPrenom = (prenom + " " + nom).trim();


        String codeBarre = data.get("codeBarre") != null
                ? data.get("codeBarre").toString()
                : null;


        List<String> privileges = new ArrayList<>();

        Object privilegesObject = data.get("privilieges");

        if (privilegesObject instanceof List) {

            for (Object privilege : (List<?>) privilegesObject) {

                if (privilege == null) {
                    continue;
                }

                String code = privilege.toString().trim();

                if (!code.isEmpty() && !privileges.contains(code)) {
                    privileges.add(code);
                }
            }
        }

        boolean admin = false;

        boolean hasBuanderieAccess =
                admin || privileges.contains("LIG0");

        if (!hasBuanderieAccess) {
            throw new RuntimeException(
                    "Accès refusé au module Buanderie"
            );
        }


        UserDTO userDTO = new UserDTO(
                userId,
                username,
                nomPrenom,
                codeBarre,
                null,
                null
        );

        SecurityUser securityUser =
                new SecurityUser(
                        username,
                        admin,
                        privileges
                );

        String token = jwtService.generateToken(securityUser);

        return new LoginResponseDTO(
                token,
                username,
                admin,
                privileges,
                codeBarre,
                userDTO
        );
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