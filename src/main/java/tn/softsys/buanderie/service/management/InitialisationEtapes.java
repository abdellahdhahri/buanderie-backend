package tn.softsys.buanderie.service.management;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.softsys.buanderie.entity.LingeEtape;
import tn.softsys.buanderie.entity.LingeEtapeSuivante;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.repository.LingeEtapeRepository;
import tn.softsys.buanderie.repository.LingeEtapeSuivanteRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialisationEtapes {

    private final LingeEtapeRepository etapeRepository;
    private final LingeEtapeSuivanteRepository suivanteRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialiser() {
        try {
            LingeEtape[] etapes = new LingeEtape[CodeEtape.values().length];

            for (CodeEtape code : CodeEtape.values()) {
                etapes[code.ordinal()] = etapeRepository.findByCode(code)
                        .map(existante -> synchroniser(existante, code))
                        .orElseGet(() -> creer(code));
            }

            if (suivanteRepository.count() == 0) {
                for (int i = 0; i < etapes.length - 1; i++) {
                    LingeEtapeSuivante lien = new LingeEtapeSuivante();
                    lien.setEtape(etapes[i]);
                    lien.setEtapeSuivante(etapes[i + 1]);
                    suivanteRepository.save(lien);
                }
            }
        } catch (Exception e) {
            log.error("Initialisation des étapes du parcours échouée : {}", e.getMessage(), e);
        }
    }

    private LingeEtape synchroniser(LingeEtape etape, CodeEtape code) {
        boolean modifie = false;

        if (!Boolean.valueOf(code.isObligatoire()).equals(etape.getObligatoire())) {
            etape.setObligatoire(code.isObligatoire());
            modifie = true;
        }
        if (code.isObligatoire() && Boolean.FALSE.equals(etape.getActif())) {
            etape.setActif(true);
            modifie = true;
            log.warn("L'étape « {} » est obligatoire : elle a été réactivée.", code.getNom());
        }
        if (etape.getPrivilegeRequis() == null) {
            etape.setPrivilegeRequis(code.getPrivilegeRequis());
            modifie = true;
        }

        return modifie ? etapeRepository.save(etape) : etape;
    }

    private LingeEtape creer(CodeEtape code) {
        int ordre = code.getOrdreParDefaut();
        if (etapeRepository.existsByOrdre(ordre)) {
            Integer max = etapeRepository.findMaxOrdre();
            ordre = (max == null ? 0 : max) + 1;
        }

        String codeBarre = String.format("E%04d", ordre);
        if (etapeRepository.existsByCodeBarre(codeBarre)) {
            codeBarre = "ETAPE-" + code.name();
        }

        LingeEtape etape = new LingeEtape();
        etape.setCode(code);
        etape.setNom(code.getNom());
        etape.setCodeBarre(codeBarre);
        etape.setCategorie("STANDARD");
        etape.setZone(code.getZone());
        etape.setOrdre(ordre);
        etape.setActif(true);
        etape.setObligatoire(code.isObligatoire());
        etape.setPrivilegeRequis(code.getPrivilegeRequis());

        return etapeRepository.save(etape);
    }
}
