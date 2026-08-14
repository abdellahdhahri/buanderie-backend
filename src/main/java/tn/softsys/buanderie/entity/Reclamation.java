package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.TypeReclamation;

import java.time.LocalDateTime;

@Entity
@Table(name = "reclamations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Linge linge;

    @ManyToOne
    private Agent agent;

    @Enumerated(EnumType.STRING)
    private TypeReclamation type;

    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    private StatutReclamation status;


    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }

    public enum StatutReclamation {
        OUVERTE, EN_COURS, RESOLUE, FERMEE
    }

    public enum PrioriteReclamation {
        BASSE, NORMALE, HAUTE, URGENTE
    }
}
