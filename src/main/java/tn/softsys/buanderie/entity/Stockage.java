package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stockages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stockage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bac_id", nullable = false)
    private Bac bac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "emplacement", length = 50)
    private String emplacement; // numéro d'étagère / zone

    @Column(name = "date_stockage", nullable = false)
    private LocalDateTime dateStockage;

    @Column(name = "date_sortie")
    private LocalDateTime dateSortie;

    @Column(length = 300)
    private String observations;

    @PrePersist
    protected void onCreate() {
        if (this.dateStockage == null) {
            this.dateStockage = LocalDateTime.now();
        }
    }
}
