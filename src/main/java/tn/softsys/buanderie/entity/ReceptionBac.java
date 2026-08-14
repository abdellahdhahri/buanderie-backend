package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

import java.time.LocalDateTime;

@Entity
@Table(name = "receptionsBacs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionBac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codeBac;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    private String chambre;

    @Enumerated(EnumType.STRING)
    private TypeCollecte typeCollecte;


    @ManyToOne
    private Agent agent;

    private LocalDateTime dateReception;

    @PrePersist
    public void init() {
        this.dateReception = LocalDateTime.now();
    }
}