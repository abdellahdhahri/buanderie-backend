package tn.softsys.buanderie.dto;

import tn.softsys.buanderie.entity.enums.NiveauRattachement;

import lombok.Data;
import tn.softsys.buanderie.entity.enums.TypeEtablissement;

@Data
public class IdentiteRequest {

    private String raisonSociale;
    private String nomApplication;
    private TypeEtablissement typeEtablissement;
    private Boolean circuitInfecteActif;
    private NiveauRattachement niveauRattachement;
    private String couleurPrimaire;
    private String adresse;
    private String telephone;
    private String email;
    private String piedDePage;
}
