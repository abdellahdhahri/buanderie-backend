package tn.softsys.buanderie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "produits_nettoyage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitNettoyage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String designation;

    @Column(name = "code_barre", unique = true, length = 100)
    private String codeBarre;

    /** Dosage recommandé (ex. 7.5). */
    private Double dosage;

    /** Dilution (ex. 100). */
    private Double dilution;

    @Column(name = "volume_total")
    private Double volumeTotal;

    @Column(name = "concentration_active")
    private Double concentrationActive;

    /** Durée d'utilisation après ouverture, en jours. */
    @Column(name = "duree_utilisation_apres_ouverture")
    private Integer dureeUtilisationApresOuverture;

    /** Température de stockage en °C. */
    @Column(name = "temperature_stockage")
    private Double temperatureStockage;

    /** LIQUIDE, POUDRE, GEL… */
    @Column(name = "type_produit", length = 30)
    private String typeProduit;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;
}
