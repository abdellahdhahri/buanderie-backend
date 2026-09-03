package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.Sqlserver.projection.LaveurImportableProjection;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.LotArticleImmobilisation;

import java.util.List;

@Repository
public interface LaveurImportRepository
        extends JpaRepository<LotArticleImmobilisation, Long> {
//
//    @Query(value = """
//        SELECT
//            LAI.ID AS id,
//            LAI.Designation AS designation,
//            LA.EtatActif AS etatActif,
//            LA.PrestataireArticleID AS prestataireArticleId,
//            A.ArticleFamilleID AS articleFamilleId,
//            LA.NSerie AS numeroSerie
//        FROM LotArticle_Immobilistation LAI
//        LEFT JOIN LotArticle LA
//            ON LAI.LotArticleID = LA.ID
//        LEFT JOIN PerstataireArticles PA
//            ON LA.PrestataireArticleID = PA.ID
//        LEFT JOIN Article A
//            ON PA.ArticleID = A.ID
//        LEFT JOIN ArticleFamille AF
//            ON A.ArticleFamilleID = AF.ID
//        WHERE
//            LA.EtatActif <> 1
//            AND LA.EtatActif <> 10
//            AND LAI.Categorie = 0
//            AND AF.Linge = 1
//        """, nativeQuery = true)
//    List<LaveurImportableProjection> findLaveursImportables();


@Query(value = """
    SELECT
        LAI.ID AS id,
        LAI.Designation AS designation,
        LAI.CodeBarre AS codeBarre,
        LA.EtatActif AS etatActif,
        LA.PrestataireArticleID AS prestataireArticleId,
        A.ArticleFamilleID AS articleFamilleId,
        LA.NSerie AS numeroSerie
    FROM LotArticle_Immobilistation LAI
    LEFT JOIN LotArticle LA
        ON LAI.LotArticleID = LA.ID
    LEFT JOIN PerstataireArticles PA
        ON LA.PrestataireArticleID = PA.ID
    LEFT JOIN Article A
        ON PA.ArticleID = A.ID
    LEFT JOIN ArticleFamille AF
        ON A.ArticleFamilleID = AF.ID
    WHERE
        LA.EtatActif <> 1
        AND LA.EtatActif <> 10
        AND LAI.Categorie = 0
        AND A.ArticleFamilleID = 22
    """, nativeQuery = true)
List<Object[]> MachinesAimporter();

    @Query(value = """
    SELECT
        LAI.ID AS id,
        LAI.Designation AS designation,
        LAI.CodeBarre AS codeBarre,
        LA.EtatActif AS etatActif,
        LA.PrestataireArticleID AS prestataireArticleId,
        A.ArticleFamilleID AS articleFamilleId,
        LA.NSerie AS numeroSerie
    FROM LotArticle_Immobilistation LAI
    LEFT JOIN LotArticle LA
        ON LAI.LotArticleID = LA.ID
    LEFT JOIN PerstataireArticles PA
        ON LA.PrestataireArticleID = PA.ID
    LEFT JOIN Article A
        ON PA.ArticleID = A.ID
    WHERE
        LA.EtatActif <> 1
        AND LA.EtatActif <> 10
        AND LAI.Categorie = 0
        AND A.ArticleFamilleID = 22
        AND LAI.ID IN (:ids)
    """, nativeQuery = true)
    List<LaveurImportableProjection> findMachinesBuanderieByIds(
            @Param("ids") List<Long> ids
    );
}