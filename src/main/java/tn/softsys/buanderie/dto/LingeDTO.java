package tn.softsys.buanderie.dto;
import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.EtatLinge;

@Data @Builder
public class LingeDTO {

    private Long id;
    private String categorie;
    private Long chambre_id;
    private Double poids;
    private EtatLinge etat;
    private String codebarre;
    private String rfidTag;
    private String programmeLavage;
    private String programmeSechage;
    // private Long lit_id;
    private Long serviceId;
    private String serviceNom;
    private  Integer durevie;
    private String serviceCode;
    private Long bacId;
    private String bacCode;
    private Long porteurId;
    private String porteurNom;
}
