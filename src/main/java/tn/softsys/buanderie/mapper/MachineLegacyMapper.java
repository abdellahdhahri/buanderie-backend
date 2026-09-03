package tn.softsys.buanderie.mapper;

import org.springframework.stereotype.Component;
import tn.softsys.buanderie.Sqlserver.dtos.AutoclaveLegacyDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Autoclave;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.LotArticleImmobilisation;
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatTechniqueMachine;
import tn.softsys.buanderie.entity.enums.TypeMachine;

@Component
public class MachineLegacyMapper {

    public Machine convertir(
            Autoclave source,
            LotArticleImmobilisation immobilisation
    ) {

        return Machine.builder()
                .id(source.getId())
                .code(source.getCodeBarre())

                .designation(
                        immobilisation != null
                                ? immobilisation.getDesignation()
                                : null
                )

                .type(TypeMachine.LAVEUR)

                .nom(source.getNom())
                .description(source.getDescription())
                .localisation(source.getEmplacement())
                .cliniqueId(source.getCliniqueId())

                .legacyLotArticleImmobilisationId(
                        source.getLotArticleImmobilisationId()
                )

                .numLot(source.getNumLot())

                .etatTechnique(
                        convertirEtatTechnique(source.getEtat())
                )

                .etatImmobilisation(
                        immobilisation != null
                                ? convertirEtatImmobilisation(
                                immobilisation.getEtatActif()
                        )
                                : null
                )

                .dateDebut(source.getDateDebut())
                .dateArret(source.getDateArret())
                .duree(source.getDuree())
                .dateTestBowieDick(source.getDateTestBowieDick())
                .dateTestVide(source.getDateTestVide())
                .personnelNom(source.getPersonnelNom())
                .nombreCycles(source.getNCycle())

                .build();
    }

    private EtatTechniqueMachine convertirEtatTechnique(Integer valeur) {

        if (valeur == null)
            return null;

        return switch (valeur) {
            case 0 -> EtatTechniqueMachine.FONCTIONNEL;
            case 1 -> EtatTechniqueMachine.FONCTIONNEL_AVEC_PROBLEME;
            case 2 -> EtatTechniqueMachine.EN_PANNE;
            case 3 -> EtatTechniqueMachine.EN_COURS_DE_REPARATION;
            case 4 -> EtatTechniqueMachine.A_CHANGER;
            case 5 -> EtatTechniqueMachine.NON_UTILISABLE;
            case 6 -> EtatTechniqueMachine.MISE_AU_REBUT;
            case 7 -> EtatTechniqueMachine.HORS_SERVICE;

            default ->
                    throw new IllegalArgumentException(
                            "Etat technique legacy inconnu : " + valeur
                    );
        };
    }

    private EtatImmobilisation convertirEtatImmobilisation(Integer valeur) {

        if (valeur == null)
            return null;

        return switch (valeur) {
            case 0 -> EtatImmobilisation.DISPONIBLE;
            case 4 -> EtatImmobilisation.EN_COURS;
            case 1 -> EtatImmobilisation.VENDU;
            case 2 -> EtatImmobilisation.PERDU;
            case 5 -> EtatImmobilisation.MISE_AU_REBUT;
            case 6 -> EtatImmobilisation.VOLE;
            case 10 -> EtatImmobilisation.ANNULE;

            default ->
                    throw new IllegalArgumentException(
                            "Etat immobilisation legacy inconnu : " + valeur
                    );
        };
    }
}