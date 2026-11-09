package tn.softsys.buanderie.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(
        name = "Service",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_service_source_id_externe", columnNames = {"source", "id_externe"})
        }
)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entité représentant un service")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String description;

    private String responsable;

    @Column(name = "type_service")
    private String typeService;

    @Column(name = "code_service", nullable = true)
    private String codeService;

    @Column(length = 60)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SourceDonnee source;

    @Column(name = "id_externe", length = 60)
    private String idExterne;

    @Builder.Default
    private Boolean actif = true;

    private LocalDateTime dateSynchro;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getCode() {
        return codeService;
    }

    @PrePersist
    public void prePersist() {
        if (this.codeService == null) {
            String prefix = "SER";
            if (this.nom != null && !this.nom.trim().isEmpty()) {
                prefix = this.nom.trim()
                        .toUpperCase()
                        .replaceAll("[^A-Z]", "");
                if (prefix.length() >= 3) {
                    prefix = prefix.substring(0, 3);
                }
            }
            String random = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();
            this.codeService = prefix + random;
        }
        if (this.source == null) {
            this.source = SourceDonnee.INTERNE;
        }
        if (this.actif == null) {
            this.actif = true;
        }
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}