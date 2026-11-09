package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.EtatLivraison;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "RemiseLinge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemiseLinge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numeroLivraison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecte_id", nullable = false)
    private Collecte collecte;

    @Column(name = "service_id", nullable = false)
    private Long service;

    @Column(name = "personnel_id", nullable = false)
    private Long personnelId;
    private LocalDateTime dateLivraison;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtatLivraison etat;

    @OneToMany(
            mappedBy = "remiseLinge",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<RemiseLingeItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {

        if (dateLivraison == null) {
            dateLivraison = LocalDateTime.now();
        }

        if (numeroLivraison == null) {
            numeroLivraison = "LIV-" + System.currentTimeMillis();
        }

        if (etat == null) {
            etat = EtatLivraison.EN_COURS;
        }
    }
}