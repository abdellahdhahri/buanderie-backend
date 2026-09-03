package tn.softsys.buanderie.Sqlserver.dtos;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AutoclaveLegacyDTO {

    private Long id;

    private String codeBarre;

    private String description;

    private String emplacement;

    private Long cliniqueId;

    private String nom;

    private Long lotArticleImmobilisationId;

    private Integer categorie;

    private Long numLot;

    private Integer etat;

    private LocalDateTime dateDebut;

    private LocalDateTime dateArret;

    private Integer duree;

    private LocalDateTime dateTestBowieDick;

    private LocalDateTime dateTestVide;

    private String personnelNom;

    private Integer nCycle;


    private Integer etatActif;
}