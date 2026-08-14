package tn.softsys.buanderie.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.StatutCycle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cycle_lavage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleLavage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProgrammeLavage programmeLavage;

    @ManyToOne(fetch = FetchType.LAZY)
    private Agent agent;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    private StatutCycle statut;

    private Double poidsCharge;

    @Builder.Default
    @OneToMany(mappedBy = "cycleLavage", cascade = CascadeType.ALL)
    @JsonManagedReference

    private List<CycleLavageItem> items = new ArrayList<>();

    @PrePersist
    public void init() {

        if (numeroCycle == null) {
            numeroCycle = "CL-" + System.currentTimeMillis();
        }

        if (dateDebut == null) {
            dateDebut = LocalDateTime.now();
        }

        if (statut == null) {
            statut = StatutCycle.EN_ATTENTE;
        }

        if (poidsCharge == null) {
            poidsCharge = 0.0;
        }
    }
}