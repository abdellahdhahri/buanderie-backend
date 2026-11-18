package tn.softsys.buanderie.dto;

import java.util.List;

public class LoginResponseDTO {

    private String token;
    private String login;
    private boolean administrateur;
    private List<String> privileges;
    private String codeBarre;
    private UserDTO user;
    private List<java.util.Map<String, Object>> etapes;

    public LoginResponseDTO(
            String token,
            String login,
            boolean administrateur,
            List<String> privileges,
            String codeBarre,
            UserDTO user) {

        this.token = token;
        this.login = login;
        this.administrateur = administrateur;
        this.privileges = privileges;
        this.codeBarre = codeBarre;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public boolean isAdministrateur() {
        return administrateur;
    }

    public void setAdministrateur(boolean administrateur) {
        this.administrateur = administrateur;
    }

    public List<String> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<String> privileges) {
        this.privileges = privileges;
    }

    public String getCodeBarre() {
        return codeBarre;
    }

    public void setCodeBarre(String codeBarre) {
        this.codeBarre = codeBarre;
    }

    public List<java.util.Map<String, Object>> getEtapes() {
        return etapes;
    }

    public void setEtapes(List<java.util.Map<String, Object>> etapes) {
        this.etapes = etapes;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}