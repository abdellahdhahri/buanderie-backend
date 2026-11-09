package tn.softsys.buanderie.service.referentiel;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import tn.softsys.buanderie.entity.enums.CategLinge;

@Service
public class CategLingeResolver {

    private static final Map<CategLinge, List<String>> MOTS_CLES = new LinkedHashMap<>();

    static {
        MOTS_CLES.put(CategLinge.TAIE_OREILLER, List.of("TAIE"));
        MOTS_CLES.put(CategLinge.COUETTE, List.of("COUETTE"));
        MOTS_CLES.put(CategLinge.CHAMP_OPERATOIRE, List.of("CHAMP"));
        MOTS_CLES.put(CategLinge.DRAP, List.of("DRAP"));
        MOTS_CLES.put(CategLinge.UNIFORME, List.of("BLOUSE", "TENUE", "PYJAMA", "CASAQUE", "UNIFORME"));
        MOTS_CLES.put(CategLinge.PEIGNOIR, List.of("PEIGNOIR"));
        MOTS_CLES.put(CategLinge.NAPPE, List.of("NAPPE"));
    }

    public CategLinge resoudre(String designation) {
        String texte = normaliser(designation);
        if (texte.isEmpty()) {
            return CategLinge.AUTRE;
        }

        if (texte.contains("SERVIETTE")) {
            if (texte.contains("BAIN")) return CategLinge.SERVIETTE_BAIN;
            if (texte.contains("TABLE")) return CategLinge.SERVIETTE_TABLE;
            return CategLinge.SERVIETTE_VISAGE;
        }

        for (Map.Entry<CategLinge, List<String>> entry : MOTS_CLES.entrySet()) {
            for (String mot : entry.getValue()) {
                if (texte.contains(mot)) {
                    return entry.getKey();
                }
            }
        }

        return CategLinge.AUTRE;
    }

    private String normaliser(String valeur) {
        if (valeur == null) {
            return "";
        }
        return Normalizer.normalize(valeur, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\'', ' ')
                .toUpperCase()
                .trim();
    }
}
