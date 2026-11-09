package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.converter.MapJsonConverter;
import tn.softsys.buanderie.entity.enums.Referentiel;

@Entity
@Table(name = "endpoint_integration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointIntegration {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Referentiel referentiel;

    @Column(length = 500)
    private String chemin;

    @Column(length = 200)
    private String cheminListe;

    @Builder.Default
    @Convert(converter = MapJsonConverter.class)
    @Column(columnDefinition = "text")
    private Map<String, String> correspondanceChamps = new LinkedHashMap<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = false;

    private LocalDateTime derniereSynchro;

    @Column(length = 20)
    private String dernierStatut;

    @Column(length = 500)
    private String dernierMessage;

    @JsonProperty("synchronisationDisponible")
    public boolean isSynchronisationDisponible() {
        return referentiel != null && referentiel.isSynchronisationDisponible();
    }
}