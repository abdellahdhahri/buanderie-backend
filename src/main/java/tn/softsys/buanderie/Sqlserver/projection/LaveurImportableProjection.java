package tn.softsys.buanderie.Sqlserver.projection;

public interface LaveurImportableProjection {

    Long getId();

    String getDesignation();

    String getCodeBarre();

    Integer getEtatActif();

    Long getPrestataireArticleId();

    Long getArticleFamilleId();

    String getNumeroSerie();
}