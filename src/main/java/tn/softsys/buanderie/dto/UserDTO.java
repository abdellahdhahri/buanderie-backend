package tn.softsys.buanderie.dto;

public class UserDTO {

    private Long id;
    private String login;
    private String nomPrenom;
    private Long profilId;
    private String profilNom;
    private String codeBarre;

    public UserDTO() {
    }

    public UserDTO(
            Long id,
            String login,
            String nomPrenom,
            String codeBarre,
            Long profilId,
            String profilNom) {

        this.id = id;
        this.login = login;
        this.nomPrenom = nomPrenom;
        this.codeBarre = codeBarre;
        this.profilId = profilId;
        this.profilNom = profilNom;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getNomPrenom() {
        return nomPrenom;
    }

    public Long getProfilId() {
        return profilId;
    }

    public String getProfilNom() {
        return profilNom;
    }

    public String getCodeBarre() {
        return codeBarre;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setNomPrenom(String nomPrenom) {
        this.nomPrenom = nomPrenom;
    }

    public void setProfilId(Long profilId) {
        this.profilId = profilId;
    }

    public void setProfilNom(String profilNom) {
        this.profilNom = profilNom;
    }

    public void setCodeBarre(String codeBarre) {
        this.codeBarre = codeBarre;
    }
}