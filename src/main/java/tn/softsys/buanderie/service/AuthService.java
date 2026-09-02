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


        String loginUrl =
                baseUrl + "/Login/Index";

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("Login", login);
        form.add("Password", password);

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form, headers);

        ResponseEntity<String> response;

        try {

            response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de l'authentification ASP.NET",
                    e
            );
        }


        List<String> cookies =
                response.getHeaders()
                        .get(HttpHeaders.SET_COOKIE);

        if (cookies == null || cookies.isEmpty()) {

            throw new RuntimeException(
                    "Aucun cookie retourné par ASP.NET après le login"
            );
        }


        String sessionId = null;

        for (String cookie : cookies) {

            if (cookie != null &&
                    cookie.startsWith("ASP.NET_SessionId=")) {

                sessionId = cookie
                        .substring("ASP.NET_SessionId=".length())
                        .split(";")[0];

                break;
            }
        }


        if (sessionId == null || sessionId.isBlank()) {

            throw new RuntimeException(
                    "ASP.NET_SessionId introuvable après authentification"
            );
        }


        String connectedUrl =
                baseUrl + "/Login/IsConnected";


        HttpHeaders sessionHeaders =
                new HttpHeaders();

        sessionHeaders.add(
                HttpHeaders.COOKIE,
                "ASP.NET_SessionId=" + sessionId
        );


        HttpEntity<Void> sessionRequest =
                new HttpEntity<>(sessionHeaders);


        ResponseEntity<String> connectedResponse;

        try {

            connectedResponse = restTemplate.exchange(
                    connectedUrl,
                    HttpMethod.GET,
                    sessionRequest,
                    String.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Impossible de vérifier la session ASP.NET",
                    e
            );
        }


        String connected =
                connectedResponse.getBody();


        if (!"OK".equalsIgnoreCase(
                connected != null
                        ? connected.trim()
                        : ""
        )) {

            throw new RuntimeException(
                    "La session ASP.NET n'est pas connectée"
            );
        }


        String privilegesUrl =
                UriComponentsBuilder
                        .fromHttpUrl(
                                baseUrl + "/Login/Mobile"
                        )
                        .queryParam("a", login)
                        .queryParam("b", password)
                        .queryParam("v", "1")
                        .toUriString();


        ResponseEntity<List> privilegesResponse;

        try {

            privilegesResponse =
                    restTemplate.exchange(
                            privilegesUrl,
                            HttpMethod.GET,
                            null,
                            List.class
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de récupération des privilèges ASP.NET",
                    e
            );
        }



        List responseData =
                privilegesResponse.getBody();

        if (responseData == null ||
                responseData.size() < 3) {

            throw new RuntimeException(
                    "Réponse des privilèges ASP.NET invalide"
            );
        }



        Object statusObject =
                responseData.get(0);

        String status =
                statusObject != null
                        ? statusObject.toString().trim()
                        : "";


        if (!"-[OK]-".equalsIgnoreCase(status)) {

            throw new RuntimeException(
                    "ASP.NET a refusé la récupération des privilèges"
            );
        }


        Object adminObject =
                responseData.get(2);

        String adminValue =
                adminObject != null
                        ? adminObject.toString().trim()
                        : "0";


        boolean admin =
                "1".equals(adminValue);




        List<String> privileges =
                new ArrayList<>();


        for (int i = 3; i < responseData.size(); i++) {

            Object item =
                    responseData.get(i);

            if (item == null) {
                continue;
            }


            String privilege =
                    item.toString().trim();


            if (!privilege.isEmpty() &&
                    !privileges.contains(privilege)) {

                privileges.add(privilege);
            }
        }



        boolean hasBuanderieAccess =
                admin ||
                        privileges.contains("LIG0");


        if (!hasBuanderieAccess) {

            throw new RuntimeException(
                    "Accès refusé au module Buanderie"
            );
        }
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
                null,
                null
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
}