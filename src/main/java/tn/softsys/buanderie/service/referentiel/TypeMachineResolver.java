package tn.softsys.buanderie.service.referentiel;

import java.text.Normalizer;

import org.springframework.stereotype.Component;

import tn.softsys.buanderie.entity.enums.TypeMachine;

@Component
public class TypeMachineResolver {

    public TypeMachine resoudre(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }

        String normalise = Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();

        boolean laveur = false;
        boolean secheur = false;

        for (String mot : normalise.split("[^A-Z]+")) {
            if (mot.startsWith("LAV") || mot.startsWith("WASH")) {
                laveur = true;
            }
            if (mot.startsWith("SECH") || mot.startsWith("DRY") || mot.startsWith("TUMBLE")) {
                secheur = true;
            }
        }

        if (laveur && secheur) {
            return TypeMachine.LAVEUR_SECHEUR;
        }
        if (laveur) {
            return TypeMachine.LAVEUR;
        }
        if (secheur) {
            return TypeMachine.SECHEUR;
        }
        return null;
    }
}
