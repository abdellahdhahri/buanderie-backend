package tn.softsys.buanderie.service.management;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.ChambreRequest;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.Lit;


import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.LitRepository;
import tn.softsys.buanderie.repository.ServiceRepository;
import tn.softsys.buanderie.service.referentiel.CleReferentiel;
import tn.softsys.buanderie.service.referentiel.IntegrationService;

@Service
@RequiredArgsConstructor
@Transactional
public class ChambreService {

    private final ChambreRepository chambreRepository;
    private final LitRepository litRepository;
    private final LingeRepository lingeRepository;
    private final ServiceRepository serviceRepository;
    private final IntegrationService integrationService;

    @Transactional(readOnly = true)
    public List<Chambre> findAll() {
        return findAll(false);
    }

    @Transactional(readOnly = true)
    public List<Chambre> findAll(boolean tous) {
        return tous ? chambreRepository.findAll() : chambreRepository.findActives();
    }

    @Transactional(readOnly = true)
    public Chambre findById(Long id) {
        return chambreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre", id));
    }

    @Transactional(readOnly = true)
    public Chambre findByCodeBarre(String codeBarre) {
        return chambreRepository.findByCodeBarre(codeBarre.trim())
                .orElseThrow(() -> new BusinessException("Chambre introuvable : " + codeBarre));
    }

    public Chambre creer(ChambreRequest request) {
        if (integrationService.estImporte(Referentiel.CHAMBRE)) {
            throw new BusinessException(
                    "Les chambres sont importées depuis le système externe : ajoutez-les dans le système source.");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessException("Le code de la chambre est obligatoire.");
        }
        verifierService(request.getServiceId());

        String codeBarre = vide(request.getCodeBarre()) ? null : request.getCodeBarre().trim();
        if (codeBarre != null && chambreRepository.existsByCodeBarre(codeBarre)) {
            throw new BusinessException("Ce code-barres est déjà utilisé par une autre chambre.");
        }

        Chambre chambre = chambreRepository.save(Chambre.builder()
                .code(request.getCode().trim())
                .codeBarre(codeBarre)
                .serviceId(request.getServiceId())
                .actif(request.getActif() == null || request.getActif())
                .source(SourceDonnee.INTERNE)
                .build());

        if (Boolean.TRUE.equals(request.getCreerLitParDefaut())) {
            litRepository.save(Lit.builder()
                    .chambre(chambre)
                    .code(chambre.getCode())
                    .type("SIMPLE")
                    .actif(true)
                    .infecte(false)
                    .source(SourceDonnee.INTERNE)
                    .build());
        }

        return chambre;
    }

    public Chambre modifier(Long id, ChambreRequest request) {
        Chambre chambre = findById(id);

        verifierService(request.getServiceId());
        chambre.setServiceId(request.getServiceId());

        if (chambre.getSource() == SourceDonnee.EXTERNE) {
            return chambreRepository.save(chambre);
        }

        if (!vide(request.getCode())) {
            chambre.setCode(request.getCode().trim());
        }

        if (!vide(request.getCodeBarre()) && !request.getCodeBarre().trim().equals(chambre.getCodeBarre())) {
            String codeBarre = request.getCodeBarre().trim();
            boolean pris = chambreRepository.findByCodeBarre(codeBarre)
                    .filter(autre -> !autre.getId().equals(chambre.getId()))
                    .isPresent();
            if (pris) {
                throw new BusinessException("Ce code-barres est déjà utilisé par une autre chambre.");
            }
            chambre.setCodeBarre(codeBarre);
        }

        if (request.getActif() != null && !request.getActif().equals(chambre.getActif())) {
            if (!request.getActif()) {
                verifierSansLinge(chambre);
            }
            chambre.setActif(request.getActif());
        }

        return chambreRepository.save(chambre);
    }

    public void desactiver(Long id) {
        Chambre chambre = findById(id);

        if (chambre.getSource() == SourceDonnee.EXTERNE) {
            throw new BusinessException(
                    "Cette chambre est synchronisée depuis le système externe : désactivez-la dans le système source.");
        }

        verifierSansLinge(chambre);

        chambre.setActif(false);
        chambreRepository.save(chambre);

        for (Lit lit : litRepository.findByChambreId(id)) {
            lit.setActif(false);
            litRepository.save(lit);
        }
    }

    private void verifierService(Long serviceId) {
        if (serviceId != null && !serviceRepository.existsById(serviceId)) {
            throw new BusinessException("Service introuvable : " + serviceId);
        }
    }

    private void verifierSansLinge(Chambre chambre) {
        List<Long> cles = litRepository.findByChambreId(chambre.getId()).stream()
                .map(l -> CleReferentiel.cle(l))
                .toList();
        if (cles.isEmpty()) {
            return;
        }
        int nombre = lingeRepository.findByLitIdInAndEtat(cles, EtatLinge.LIVRE).size();
        if (nombre > 0) {
            throw new BusinessException(
                    "Impossible : " + nombre + " linge(s) sont affectés aux lits de cette chambre.");
        }
    }

    private boolean vide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
