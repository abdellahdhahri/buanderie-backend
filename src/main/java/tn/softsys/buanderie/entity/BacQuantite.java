package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Entity
@Table(name = "bac_quantites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacQuantite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bac_id", nullable = false)
    private Bac bac;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategLinge categorie;

    @Column(name = "quantite_declaree", nullable = false)
    private Integer quantiteDeclaree;

    @Column(name = "collecte_id")
    private Long collecteId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "personnel_id")
    private Long personnelId;

    @Column(length = 100)
    private String chambre;

    @Column(length = 500)
    private String observations;

    @Column(name = "date_declaration")
    private LocalDateTime dateDeclaration;
}
