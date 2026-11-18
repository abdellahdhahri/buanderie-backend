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




    public LoginResponseDTO login(String login, String password) {

        if (login == null || login.isBlank()) {
            throw new BusinessException("Login obligatoire");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("Mot de passe obligatoire");
        }

        String identifiant = login.trim();
        CubixClient.Connexion connexion = cubixClient.seConnecter(identifiant, password);
        return ouvrirSession(identifiant, connexion.jeton(), connexion.privileges());
    }

    /** Connexion par badge : CUBIX donne le jeton du compte lié au badge (voir Login/MobileBadge). */
    public LoginResponseDTO loginByBadge(String codeBarre) {
        if (codeBarre == null || codeBarre.isBlank()) {
            throw new BusinessException("Code barre obligatoire");
        }
        CubixClient.ConnexionBadge connexion = cubixClient.seConnecterParBadge(codeBarre);
        return ouvrirSession(connexion.login(), connexion.jeton(), connexion.privileges());
    }

    private LoginResponseDTO ouvrirSession(String identifiant, String jeton, List<String> privileges) {
        CubixClient.Identite identite = cubixClient.ping(jeton);
        List<CubixClient.Etape> etapes = cubixClient.etapes(jeton);

        boolean admin = identite.administrateur();
        if (!admin && (privileges == null || privileges.isEmpty())) {
            throw new BusinessException("Votre compte n'a aucun droit d'accès au module Buanderie. "
                    + "Demandez à un administrateur de vous attribuer un privilège (LIG...).");
        }
        List<CubixClient.Etape> habilitees = etapesHabilitees(admin, identite.id(), etapes);
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

}