package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pliages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pliage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bac_id", nullable = false)
    private Bac bac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "date_pliage", nullable = false)
    private LocalDateTime datePliage;

    @Column(length = 300)
    private String observations;

    @PrePersist
    protected void onCreate() {
        if (this.datePliage == null) {
            this.datePliage = LocalDateTime.now();
        }
    }
}
