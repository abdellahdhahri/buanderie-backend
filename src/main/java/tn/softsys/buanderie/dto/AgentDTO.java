package tn.softsys.buanderie.dto;

import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.Agent;

import java.time.LocalDateTime;

@Data
@Builder
public class AgentDTO {
    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String poste;
    private String telephone;
    private boolean actif;
    private LocalDateTime dateCreation;

    public static AgentDTO from(Agent a) {
        if (a == null) return null;
        return AgentDTO.builder()
                .id(a.getId())
                .matricule(a.getMatricule())
                .nom(a.getNom())
                .prenom(a.getPrenom())
                .nomComplet(a.getPrenom() + " " + a.getNom())
                .poste(a.getPoste())
                .telephone(a.getTelephone())
                .actif(a.isActif())
                .dateCreation(a.getDateCreation())
                .build();
    }
}
