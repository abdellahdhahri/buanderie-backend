package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.softsys.buanderie.entity.enums.TypeEtablissement;

@Entity
@Table(name = "identite_etablissement")
@Getter
@Setter
@NoArgsConstructor
public class IdentiteEtablissement {

    public static final Long ID_UNIQUE = 1L;

    @Id
    private Long id = ID_UNIQUE;

    @Column(name = "raison_sociale", length = 255)
    private String raisonSociale;

    @Column(name = "nom_application", nullable = false, length = 100)
    private String nomApplication = "Buanderie";

    @Enumerated(EnumType.STRING)
    @Column(name = "type_etablissement", length = 20)
    private TypeEtablissement typeEtablissement;

    @Column(name = "couleur_primaire", length = 7)
    private String couleurPrimaire;

    @Column(length = 500)
    private String adresse;

    @Column(length = 50)
    private String telephone;

    @Column(length = 150)
    private String email;

    @Column(name = "pied_de_page", length = 500)
    private String piedDePage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_id", foreignKey = @ForeignKey(name = "fk_identite_logo"))
    private LogoEtablissement logo;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}
