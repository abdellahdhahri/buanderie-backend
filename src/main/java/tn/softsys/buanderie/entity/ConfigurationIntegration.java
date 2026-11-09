package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;

@Entity
@Table(name = "configuration_integration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationIntegration {

    public static final Long ID_UNIQUE = 1L;

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModeSource mode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TypeConnecteur typeConnecteur;

    @Column(length = 300)
    private String urlBase;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TypeAuthentification typeAuthentification = TypeAuthentification.AUCUNE;

    @JsonIgnore
    @Column(length = 500)
    private String jeton;

    @Column(length = 100)
    private String nomEnteteCle;

    @Column(length = 120)
    private String utilisateur;

    @JsonIgnore
    @Column(length = 200)
    private String motDePasse;

    @Builder.Default
    @Column(nullable = false)
    private Integer frequenceMinutes = 60;

    @Builder.Default
    @Column(nullable = false)
    private Boolean synchroAutomatique = true;

    private LocalDateTime dateModification;

    @JsonProperty("jetonConfigure")
    public boolean isJetonConfigure() {
        return jeton != null && !jeton.isBlank();
    }

    @JsonProperty("motDePasseConfigure")
    public boolean isMotDePasseConfigure() {
        return motDePasse != null && !motDePasse.isBlank();
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        dateModification = LocalDateTime.now();
        if (typeAuthentification == null) {
            typeAuthentification = TypeAuthentification.AUCUNE;
        }
    }
}