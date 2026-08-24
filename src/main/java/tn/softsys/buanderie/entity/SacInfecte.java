package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.EtatSac;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sacs_infectes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SacInfecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "numero_sac", unique = true, nullable = false, length = 30)
    private String numeroSac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecte_id")
    private Collecte collecte;

    private String rfidcode ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_collecte_id", nullable = false)
    private Agent agentCollecte;

    @Column(name = "service_id", nullable = true)
    private Long serviceId;

    @Column(length = 50)
    private String chambre;

    @Column(name = "quantite_declaree", nullable = false)
    private Integer quantiteDeclaree;

    @Column(name = "quantite_comptee_post_lavage")
    private Integer quantiteCompteePostLavage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EtatSac etat = EtatSac.DECLARE;

    @Column(name = "date_collecte", nullable = false)
    private LocalDateTime dateCollecte;

    @Column(name = "date_reception")
    private LocalDateTime dateReception;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_reception_id")
    private Agent agentReception;

    @Column(name = "date_lavage")
    private LocalDateTime dateLavage;

    @Column(name = "date_verification")
    private LocalDateTime dateVerification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_lavage_id")
    private Agent agentLavage;

    @Column(name = "ecart")
    private Integer ecart;

    @Column(length = 500)
    private String observations;

    @OneToMany(mappedBy = "sacInfecte", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SacInfecteItem> items = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "sac_infecte_propre_id")
    @Builder.Default
    private List<Linge> lingesPropres = new ArrayList<>();

    @Column(name = "date_affectation_propres")
    private LocalDateTime dateAffectationPropres;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_affectation_id",nullable = true)
    private Agent agentAffectation;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_lavage_id")
    private CycleLavage cycleLavage;
    @PrePersist
    protected void onCreate() {
        if (this.dateCollecte == null) this.dateCollecte = LocalDateTime.now();
        if (this.numeroSac == null)
            this.numeroSac = "SAC-INF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        if (this.etat == null) this.etat = EtatSac.DECLARE;
        if (this.rfidcode ==null)
            this.rfidcode = "RF-SI-"+ UUID.randomUUID().toString().substring(0,7 ).toUpperCase();
    }

}
