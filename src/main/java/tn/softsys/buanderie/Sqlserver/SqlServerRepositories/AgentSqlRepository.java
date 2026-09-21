package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.AgentSql;

public interface AgentSqlRepository extends JpaRepository<AgentSql, Long> {

    @Query(
            value = """
            SELECT
                p.ID AS ID,
                p.Nom AS Nom,
                p.Prenom AS Prenom,
                p.NomPrenom AS NomPrenom,
                p.Matricule AS Matricule,
                p.CodeBarre AS CodeBarre,

                p.FonctionID AS FonctionID,
                f.Code AS FonctionCode,

                p.ServiceID AS ServiceID,
                s.Code AS ServiceCode,

                p.PointeuseActif AS PointeuseActif,
                p.Administrateur AS Administrateur,
                p.Visible AS Visible

            FROM Personnel p

            LEFT JOIN Fonction f
                ON p.FonctionID = f.ID

            LEFT JOIN Service s
                ON p.ServiceID = s.ID

            ORDER BY p.ID
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM Personnel p
            """,
            nativeQuery = true
    )
    Page<AgentSql> findAllAgents(Pageable pageable);
}