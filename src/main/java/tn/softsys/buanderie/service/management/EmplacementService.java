package tn.softsys.buanderie.service.management;

import tn.softsys.buanderie.service.referentiel.CubixArsenalImportService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.EmplacementDetailDTO;
import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.Emplacement;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ArsenalRepository;
import tn.softsys.buanderie.repository.EmplacementRepository;
import tn.softsys.buanderie.repository.LingeArsenalRepository;
import tn.softsys.buanderie.service.referentiel.CleReferentiel;
import tn.softsys.buanderie.service.referentiel.IntegrationService;

@Service
@RequiredArgsConstructor
public class EmplacementService {

    private final EmplacementRepository emplacementRepository;
    private final ArsenalRepository arsenalRepository;
    private final LingeArsenalRepository lingeArsenalRepository;
    private final IntegrationService integrationService;
    private final CubixArsenalImportService cubixArsenalImportService;

    @Transactional(readOnly = true)
    public List<EmplacementDetailDTO> lister(Long arsenalId, boolean tous) {
        List<Emplacement> emplacements;

        if (arsenalId != null) {
            if (!arsenalRepository.existsById(arsenalId)) {
                throw new BusinessException("Arsenal introuvable : " + arsenalId);
            }
            emplacements = tous
                    ? emplacementRepository.findByArsenal_IdOrderByNumeroAscNomAsc(arsenalId)
                    : emplacementRepository.findByArsenal_IdAndActifTrueOrderByNumeroAscNomAsc(arsenalId);
        } else {
            emplacements = tous
                    ? emplacementRepository.findAll()
                    : emplacementRepository.findByActifTrueOrderByArsenal_IdAscNumeroAscNomAsc();
        }

        return emplacements.stream().map(EmplacementDetailDTO::depuis).toList();
    }

    @Transactional(readOnly = true)
    public EmplacementDetailDTO trouver(Long id) {
        return EmplacementDetailDTO.depuis(charger(id));
    }

    @Transactional(readOnly = true)
    public Optional<EmplacementDetailDTO> trouverParCodeBarre(String codeBarre) {
        if (vide(codeBarre)) {
            throw new BusinessException("Le code-barres de l'emplacement est obligatoire.");
        }
        return emplacementRepository.findByCodeBarre(codeBarre.trim())
                .filter(e -> !Boolean.FALSE.equals(e.getActif()))
                .map(EmplacementDetailDTO::depuis);
    }

    @Transactional(readOnly = true)
    public Optional<EmplacementDetailDTO> trouverParCle(Long cle) {
        if (cle == null) {
            throw new BusinessException("L'identifiant de l'emplacement est obligatoire.");
        }
        Optional<Emplacement> externe = emplacementRepository
                .findBySourceAndIdExterne(SourceDonnee.EXTERNE, String.valueOf(cle));
        if (externe.isPresent()) {
            return externe.map(EmplacementDetailDTO::depuis);
        }
        return emplacementRepository.findById(cle)
                .filter(e -> Objects.equals(CleReferentiel.cle(e), cle))
                .map(EmplacementDetailDTO::depuis);
    }

    @Transactional
    public EmplacementDetailDTO creer(EmplacementDetailDTO dto) {
        if (cubixArsenalImportService.actif()) {
            // Mode CUBIX : l'emplacement est créé dans CUBIX (AjouterEmplacements) puis relu ici
            return cubixArsenalImportService.creerEmplacement(dto);
        }
        if (integrationService.estImporte(Referentiel.EMPLACEMENT)) {
            throw new BusinessException(
                    "Les emplacements sont importés depuis le système externe : ajoutez-les dans le système source.");
        }
        if (dto.getArsenalId() == null) {
            throw new BusinessException("L'arsenal de l'emplacement est obligatoire.");
        }
        if (dto.getNom() == null || dto.getNom().isBlank()) {
            throw new BusinessException("Le nom de l'emplacement est obligatoire.");
        }

        Arsenal arsenal = chargerArsenal(dto.getArsenalId());
        if (Boolean.FALSE.equals(arsenal.getActif())) {
            throw new BusinessException("Impossible d'ajouter un emplacement à un arsenal désactivé.");
        }

        String codeBarre = vide(dto.getCodeBarre()) ? null : dto.getCodeBarre().trim();
        if (codeBarre != null && emplacementRepository.existsByCodeBarre(codeBarre)) {
            throw new BusinessException("Ce code-barres est déjà utilisé par un autre emplacement.");
        }

        Emplacement emplacement = Emplacement.builder()
                .arsenal(arsenal)
                .nom(dto.getNom().trim())
                .numero(dto.getNumero())
                .codeBarre(codeBarre)
                .actif(dto.getActif() == null || dto.getActif())
                .source(SourceDonnee.INTERNE)
                .build();

        return EmplacementDetailDTO.depuis(emplacementRepository.save(emplacement));
    }

    @Transactional
    public EmplacementDetailDTO modifier(Long id, EmplacementDetailDTO dto) {
        Emplacement emplacement = charger(id);
        verifierModifiable(emplacement);

        if (!vide(dto.getNom())) {
            emplacement.setNom(dto.getNom().trim());
        }
        if (dto.getNumero() != null) {
            emplacement.setNumero(dto.getNumero());
        }

        if (!vide(dto.getCodeBarre()) && !dto.getCodeBarre().trim().equals(emplacement.getCodeBarre())) {
            String codeBarre = dto.getCodeBarre().trim();
            boolean pris = emplacementRepository.findByCodeBarre(codeBarre)
                    .filter(autre -> !autre.getId().equals(emplacement.getId()))
                    .isPresent();
            if (pris) {
                throw new BusinessException("Ce code-barres est déjà utilisé par un autre emplacement.");
            }
            emplacement.setCodeBarre(codeBarre);
        }

        if (dto.getArsenalId() != null && !dto.getArsenalId().equals(emplacement.getArsenal().getId())) {
            Arsenal arsenal = chargerArsenal(dto.getArsenalId());
            if (Boolean.FALSE.equals(arsenal.getActif())) {
                throw new BusinessException("Impossible de rattacher un emplacement à un arsenal désactivé.");
            }
            verifierSansStock(emplacement);
            emplacement.setArsenal(arsenal);
        }

        if (dto.getActif() != null && !dto.getActif().equals(emplacement.getActif())) {
            if (!dto.getActif()) {
                verifierSansStock(emplacement);
            }
            emplacement.setActif(dto.getActif());
        }

        return EmplacementDetailDTO.depuis(emplacementRepository.save(emplacement));
    }

    @Transactional
    public void desactiver(Long id) {
        Emplacement emplacement = charger(id);
        verifierModifiable(emplacement);
        verifierSansStock(emplacement);
        emplacement.setActif(false);
        emplacementRepository.save(emplacement);
    }

    private Emplacement charger(Long id) {
        return emplacementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Emplacement introuvable : " + id));
    }

    private Arsenal chargerArsenal(Long id) {
        return arsenalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Arsenal introuvable : " + id));
    }

    private void verifierModifiable(Emplacement emplacement) {
        if (emplacement.getSource() == SourceDonnee.EXTERNE) {
            throw new BusinessException(
                    "Cet emplacement est synchronisé depuis le système externe : modifiez-le dans le système source.");
        }
    }

    private void verifierSansStock(Emplacement emplacement) {
        int nombre = lingeArsenalRepository
                .findByArsenalEmplacementIDAndEtatOrderByCategorieAsc(CleReferentiel.cle(emplacement), EtatLinge.STOCKE)
                .size();
        if (nombre > 0) {
            throw new BusinessException(
                    "Impossible : " + nombre + " linge(s) sont rangés dans cet emplacement.");
        }
    }

    private boolean vide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
