package tn.softsys.buanderie.dto.Referentiel;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;

@Data
public class ConfigurationIntegrationRequest {

    @NotNull
    private ModeSource mode;

    private TypeConnecteur typeConnecteur;

    @Size(max = 300)
    private String urlBase;

    private TypeAuthentification typeAuthentification;

    @Size(max = 500)
    private String jeton;

    @Size(max = 100)
    private String nomEnteteCle;

    @Size(max = 120)
    private String utilisateur;

    @Size(max = 200)
    private String motDePasse;

    @Min(5)
    private Integer frequenceMinutes;

    private Boolean synchroAutomatique;

    @Valid
    private List<EndpointRequest> endpoints;

    @Data
    public static class EndpointRequest {

        @NotNull
        private Referentiel referentiel;

        @Size(max = 500)
        private String chemin;

        @Size(max = 200)
        private String cheminListe;

        private Map<String, String> correspondanceChamps;

        private Boolean actif;
    }
}