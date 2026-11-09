package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

@Entity
@Table(
        name = "lits",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lit_source_id_externe", columnNames = {"source", "id_externe"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "chambre")
@ToString(exclude = "chambre")
public class Lit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String reference;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 100)
    private String code;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id", nullable = false)
    @JsonIgnore
    private Chambre chambre;

    @Builder.Default
    private Boolean actif = true;

    @Builder.Default
    private Boolean infecte = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SourceDonnee source;

    @Column(name = "id_externe", length = 60)
    private String idExterne;

    private LocalDateTime dateSynchro;

    @PrePersist
    public void prePersist() {
        if (this.reference == null || this.reference.isBlank()) {
            this.reference = this.code != null && !this.code.isBlank()
                    ? (this.code.length() > 20 ? this.code.substring(0, 20) : this.code)
                    : "LIT" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
        if (this.type == null || this.type.isBlank()) {
            this.type = "SIMPLE";
        }
        if (this.source == null) {
            this.source = SourceDonnee.INTERNE;
        }
        if (this.actif == null) {
            this.actif = true;
        }
        if (this.infecte == null) {
            this.infecte = false;
        }
    }
}
