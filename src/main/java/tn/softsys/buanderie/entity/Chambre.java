package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.service.referentiel.CleReferentiel;

@Entity
@Table(
        name = "chambres",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chambre_source_id_externe", columnNames = {"source", "id_externe"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chambre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String code;

    @Column
    private Long serviceId;

    @Column
    private String codeBarre;

    @Builder.Default
    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SourceDonnee source;

    @Column(name = "id_externe", length = 60)
    private String idExterne;

    private LocalDateTime dateSynchro;

    @JsonProperty("cle")
    public Long getCle() {
        return CleReferentiel.cle(this);
    }

    @PrePersist
    public void prePersist() {
        if (this.codeBarre == null) {
            this.codeBarre = "CHB" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
        if (this.source == null) {
            this.source = SourceDonnee.INTERNE;
        }
        if (this.actif == null) {
            this.actif = true;
        }
    }
}
