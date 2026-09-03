package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.Sqlserver.dtos.AutoclaveLegacyDTO;

import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AutoclaveLegacyRepository {

    private final JdbcTemplate sqlServerJdbcTemplate;

    public List<AutoclaveLegacyDTO> findLaveurs() {

        String sql = """
                SELECT
                    A.ID,
                    A.CodeBarre,
                    A.Description,
                    A.Emplacement,
                    A.CliniqueID,
                    A.Nom,
                    A.LotArticle_ImmobilistationID,
                    A.Categorie,
                    A.NumLot,
                    A.Etat,
                    A.DateDebut,
                    A.DateArret,
                    A.Duree,
                    A.DateTestBowieDick,
                    A.DateTestVide,
                    A.PersonnelNom,
                    A.NCycle,
                    LAI.EtatActif
                FROM Autoclave A
                LEFT JOIN LotArticle_Immobilistation LAI
                    ON A.LotArticle_ImmobilistationID = LAI.ID
                WHERE A.Categorie = 2
                ORDER BY A.ID
                """;

        return sqlServerJdbcTemplate.query(sql, (rs, rowNum) -> {

            AutoclaveLegacyDTO dto = new AutoclaveLegacyDTO();

            dto.setId(rs.getLong("ID"));
            dto.setCodeBarre(rs.getString("CodeBarre"));
            dto.setDescription(rs.getString("Description"));
            dto.setEmplacement(rs.getString("Emplacement"));
            dto.setCliniqueId(getLongNullable(rs, "CliniqueID"));
            dto.setNom(rs.getString("Nom"));

            dto.setLotArticleImmobilisationId(
                    getLongNullable(rs, "LotArticle_ImmobilistationID")
            );

            dto.setCategorie(getIntegerNullable(rs, "Categorie"));
            dto.setNumLot(getLongNullable(rs, "NumLot"));
            dto.setEtat(getIntegerNullable(rs, "Etat"));

            Timestamp dateDebut = rs.getTimestamp("DateDebut");
            if (dateDebut != null) {
                dto.setDateDebut(dateDebut.toLocalDateTime());
            }

            Timestamp dateArret = rs.getTimestamp("DateArret");
            if (dateArret != null) {
                dto.setDateArret(dateArret.toLocalDateTime());
            }

            dto.setDuree(getIntegerNullable(rs, "Duree"));

            Timestamp dateBowie = rs.getTimestamp("DateTestBowieDick");
            if (dateBowie != null) {
                dto.setDateTestBowieDick(dateBowie.toLocalDateTime());
            }

            Timestamp dateVide = rs.getTimestamp("DateTestVide");
            if (dateVide != null) {
                dto.setDateTestVide(dateVide.toLocalDateTime());
            }

            dto.setPersonnelNom(rs.getString("PersonnelNom"));
            dto.setNCycle(getIntegerNullable(rs, "NCycle"));
            dto.setEtatActif(getIntegerNullable(rs, "EtatActif"));

            return dto;
        });
    }

    private Long getLongNullable(
            java.sql.ResultSet rs,
            String column
    ) throws java.sql.SQLException {

        long value = rs.getLong(column);

        return rs.wasNull() ? null : value;
    }

    private Integer getIntegerNullable(
            java.sql.ResultSet rs,
            String column
    ) throws java.sql.SQLException {

        int value = rs.getInt(column);

        return rs.wasNull() ? null : value;
    }
}