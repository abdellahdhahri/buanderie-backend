package tn.softsys.buanderie.Sqlserver.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArsenalDto {






        @JsonProperty("ArsenalID")
        private Long arsenalID;

        @JsonProperty("LotArticle_ImmobilistationID")
        private Long lotArticleImmobilistationID;

        @JsonProperty("LotArticleID")
        private Long lotArticleID;

        @JsonProperty("Designation")
        private String designation;

        @JsonProperty("CodeBarre")
        private String codeBarre;

        @JsonProperty("CategorieArsenal")
        private Integer categorieArsenal;

        @JsonProperty("CategorieImmobilisation")
        private Integer categorieImmobilisation;

        @JsonProperty("NbrCol")
        private Integer nbrCol;

        @JsonProperty("EtatActif")
        private Integer etatActif;

        @JsonProperty("EtatFonc")
        private Integer etatFonc;

        @JsonProperty("PieceID")
        private Long pieceID;

        @JsonProperty("NumPiece")
        private String numPiece;

        @JsonProperty("FournisseurID")
        private Long fournisseurID;

        @JsonProperty("FournisseurNom")
        private String fournisseurNom;
    }


