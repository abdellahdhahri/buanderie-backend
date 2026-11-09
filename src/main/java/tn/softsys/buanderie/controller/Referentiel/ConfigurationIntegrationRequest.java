package tn.softsys.buanderie.controller.Referentiel;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;

@Data
public class ConfigurationIntegrationRequest {

    @NotNull
    private ModeSource mode;

    private TypeConnecteur typeConnecteur;

    @Size(max = 300)
    private String urlBase;

    @Size(max = 500)
    private String jeton;

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

        private Boolean actif;
    }
}
