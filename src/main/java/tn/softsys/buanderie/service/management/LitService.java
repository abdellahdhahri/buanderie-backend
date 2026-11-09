package tn.softsys.buanderie.service.management;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.LitDetailDTO;
import tn.softsys.buanderie.dto.LitRequest;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.Lit;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.LitRepository;
import tn.softsys.buanderie.service.referentiel.CleReferentiel;
import tn.softsys.buanderie.service.referentiel.IntegrationService;

@Service
@RequiredArgsConstructor
public class LitService {

    private final LitRepository litRepository;
    private final ChambreRepository chambreRepository;
    private final LingeRepository lingeRepository;
    private final IntegrationService integrationService;

    @Transactional(readOnly = true)
    public List<LitDetailDTO> lister(Long chambreId, boolean tous) {
        List<Lit> lits;

        if (chambreId != null) {
            if (!chambreRepository.existsById(chambreId)) {
                throw new BusinessException("Chambre introuvable : " + chambreId);
            }
            lits = tous ? litRepository.findByChambreId(chambreId) : litRepository.findActifsByChambre(chambreId);
        } else {
            lits = tous
                    ? litRepository.findAll()
                    : litRepository.findAll().stream().filter(l -> !Boolean.FALSE.equals(l.getActif())).toList();
        }

        return lits.stream().map(LitDetailDTO::depuis).toList();
    }

    @Transactional(readOnly = true)
    public LitDetailDTO trouver(Long id) {
        return LitDetailDTO.depuis(charger(id));
    }

    @Transactional(readOnly = true)
    public Optional<LitDetailDTO> trouverParCode(String code) {
        if (vide(code)) {
            throw new BusinessException("Le code du lit est obligatoire.");
        }
        Lit lit = litRepository.findFirstByCodeOrderByIdAsc(code.trim());
        return lit == null || Boolean.FALSE.equals(lit.getActif())
                ? Optional.empty()
                : Optional.of(LitDetailDTO.depuis(lit));
    }

    @Transactional(readOnly = true)
    public List<LitDetailDTO> listerInfectes() {
        return litRepository.findInfectesActifs().stream().map(LitDetailDTO::depuis).toList();
    }

    @Transactional
    public LitDetailDTO creer(LitRequest request) {
        if (integrationService.estImporte(Referentiel.LIT)) {
            throw new BusinessException(
                    "Les lits sont importés depuis le système externe : ajoutez-les dans le système source.");
        }
        if (request.getChambreId() == null) {
            throw new BusinessException("La chambre du lit est obligatoire.");
        }

        Chambre chambre = chambreRepository.findById(request.getChambreId())
                .orElseThrow(() -> new BusinessException("Chambre introuvable : " + request.getChambreId()));
        if (Boolean.FALSE.equals(chambre.getActif())) {
            throw new BusinessException("Impossible d'ajouter un lit à une chambre désactivée.");
        }
        if (vide(request.getCode()) && vide(request.getReference())) {
            throw new BusinessException("Le code du lit est obligatoire.");
        }

        Lit lit = Lit.builder()
                .chambre(chambre)
                .code(vide(request.getCode()) ? request.getReference().trim() : request.getCode().trim())
                .reference(vide(request.getReference()) ? null : limiter(request.getReference(), 20))
                .description(vide(request.getDescription()) ? null : request.getDescription().trim())
                .type(vide(request.getType()) ? "SIMPLE" : limiter(request.getType().toUpperCase(), 20))
                .actif(request.getActif() == null || request.getActif())
                .infecte(Boolean.TRUE.equals(request.getInfecte()))
                .source(SourceDonnee.INTERNE)
                .build();

        return LitDetailDTO.depuis(litRepository.save(lit));
    }

    @Transactional
    public LitDetailDTO modifier(Long id, LitRequest request) {
        Lit lit = charger(id);

        if (lit.getSource() == SourceDonnee.EXTERNE) {
            throw new BusinessException(
                    "Ce lit est synchronisé depuis le système externe : modifiez-le dans le système source.");
        }

        if (!vide(request.getCode())) {
            lit.setCode(request.getCode().trim());
        }
        if (!vide(request.getReference())) {
            lit.setReference(limiter(request.getReference(), 20));
        }
        if (request.getDescription() != null) {
            lit.setDescription(request.getDescription().trim());
        }
        if (!vide(request.getType())) {
            lit.setType(limiter(request.getType().toUpperCase(), 20));
        }

        if (request.getChambreId() != null && !request.getChambreId().equals(lit.getChambre().getId())) {
            Chambre chambre = chambreRepository.findById(request.getChambreId())
                    .orElseThrow(() -> new BusinessException("Chambre introuvable : " + request.getChambreId()));
            if (Boolean.FALSE.equals(chambre.getActif())) {
                throw new BusinessException("Impossible de rattacher un lit à une chambre désactivée.");
            }
            verifierSansLinge(lit);
            lit.setChambre(chambre);
        }

        if (request.getActif() != null && !request.getActif().equals(lit.getActif())) {
            if (!request.getActif()) {
                verifierSansLinge(lit);
            }
            lit.setActif(request.getActif());
        }

        return LitDetailDTO.depuis(litRepository.save(lit));
    }

    @Transactional
    public LitDetailDTO definirInfection(Long id, Boolean infecte) {
        if (infecte == null) {
            throw new BusinessException("L'état d'infection est obligatoire.");
        }
        if (integrationService.estImporte(Referentiel.LIT_INFECTE)) {
            throw new BusinessException(
                    "L'état d'infection est synchronisé depuis le système externe : il ne peut pas être modifié ici.");
        }
        Lit lit = charger(id);
        lit.setInfecte(infecte);
        return LitDetailDTO.depuis(litRepository.save(lit));
    }

    @Transactional
    public void desactiver(Long id) {
        Lit lit = charger(id);

        if (lit.getSource() == SourceDonnee.EXTERNE) {
            throw new BusinessException(
                    "Ce lit est synchronisé depuis le système externe : désactivez-le dans le système source.");
        }

        verifierSansLinge(lit);
        lit.setActif(false);
        litRepository.save(lit);
    }

    private Lit charger(Long id) {
        return litRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Lit introuvable : " + id));
    }

    private void verifierSansLinge(Lit lit) {
        int nombre = lingeRepository
                .findByLitIdSqlServerInAndEtat(List.of(CleReferentiel.cle(lit)), EtatLinge.LIVRE)
                .size();
        if (nombre > 0) {
            throw new BusinessException("Impossible : " + nombre + " linge(s) sont affectés à ce lit.");
        }
    }

    private boolean vide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    private String limiter(String valeur, int max) {
        String propre = valeur.trim();
        return propre.length() > max ? propre.substring(0, max) : propre;
    }
}
