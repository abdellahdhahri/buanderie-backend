package tn.softsys.buanderie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "emplacements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_emplacement_source_id_externe", columnNames = {"source", "id_externe"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arsenal_id", nullable = false)
    @JsonIgnore
    private Arsenal arsenal;

    @Column(length = 100)
    private String nom;

    private Integer numero;

    @Column(name = "code_barre", length = 100, unique = true)
    private String codeBarre;

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
        if (this.codeBarre == null) {
            this.codeBarre = "EMP" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
        if (this.source == null) {
            this.source = SourceDonnee.INTERNE;
        }
        if (this.actif == null) {
            this.actif = true;
        }
    }
}
