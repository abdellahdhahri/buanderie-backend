package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retours_remise")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetourRemise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numeroRetour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remise_linge_id", nullable = false)
    private RemiseLinge remiseLinge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    private LocalDateTime dateRetour;

    @OneToMany(
            mappedBy = "retourRemise",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<RetourRemiseItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {

        if (dateRetour == null) {
            dateRetour = LocalDateTime.now();
        }

        if (numeroRetour == null) {
            numeroRetour = "RET-" + System.currentTimeMillis();
        }
    }
}