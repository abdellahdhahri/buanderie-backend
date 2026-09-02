package tn.softsys.buanderie.dto;

import java.util.List;

public class AuthenticatedUserDTO {

    private String login;

    private boolean admin;

    private List<String> privileges;

    public AuthenticatedUserDTO() {
    }

    public AuthenticatedUserDTO(
            String login,
            boolean admin,
            List<String> privileges) {

        this.login = login;
        this.admin = admin;
        this.privileges = privileges;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public List<String> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<String> privileges) {
        this.privileges = privileges;
    }
}