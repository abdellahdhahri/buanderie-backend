package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "retours_remise_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"retour_remise_id", "linge_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetourRemiseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retour_remise_id", nullable = false)
    private RetourRemise retourRemise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linge_id", nullable = false)
    private Linge linge;

    private LocalDateTime dateScan;

    @PrePersist
    protected void onCreate() {
        if (dateScan == null) {
            dateScan = LocalDateTime.now();
        }
    }
}