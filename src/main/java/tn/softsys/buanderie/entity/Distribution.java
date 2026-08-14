package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "livraisons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Distribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateDistribution;

    @ManyToOne
    private Agent agent;

@Column(name = "ServiceId")
    private Long serviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecte_id")
    private Collecte collecte;

    @OneToMany(mappedBy = "distribution")
    private List<DistributionItem> items;
    @PrePersist
    protected void onCreate() {
        if (this.dateDistribution == null) {
            this.dateDistribution = LocalDateTime.now();
        }
    }
}
