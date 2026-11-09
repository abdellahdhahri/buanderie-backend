package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "Arsenals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_arsenal_source_id_externe", columnNames = {"source", "id_externe"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Arsenal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String code;

    @Column(name = "service_id")
    private Long serviceId;

    @Builder.Default
    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SourceDonnee source;

    @Column(name = "id_externe", length = 60)
    private String idExterne;

    private LocalDateTime dateSynchro;

    @PrePersist
    public void prePersist() {
        if (this.code == null) {
            this.code = "ARS" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
        if (this.source == null) {
            this.source = SourceDonnee.INTERNE;
        }
        if (this.actif == null) {
            this.actif = true;
        }
    }
}
