package tn.softsys.buanderie.service.management;

import tn.softsys.buanderie.entity.enums.NiveauRattachement;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.IdentiteRequest;
import tn.softsys.buanderie.dto.IdentiteResponse;
import tn.softsys.buanderie.entity.IdentiteEtablissement;
import tn.softsys.buanderie.entity.LogoEtablissement;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.IdentiteEtablissementRepository;
import tn.softsys.buanderie.repository.LogoEtablissementRepository;

@Service
@RequiredArgsConstructor
public class IdentiteEtablissementService {

    public static final long TAILLE_MAX_LOGO = 500L * 1024;

    private final IdentiteEtablissementRepository identiteRepository;
    private final LogoEtablissementRepository logoRepository;
    private final ProfilExploitationService profilExploitationService;

    @Transactional
    public IdentiteResponse lire() {
        return versReponse(getOuCreer());
    }

    @Transactional
    public IdentiteResponse modifier(IdentiteRequest request) {
        IdentiteEtablissement identite = getOuCreer();
        boolean circuitAvant = profilExploitationService.valeurEffective(identite);
        NiveauRattachement niveauAvant = profilExploitationService.niveauEffectif(identite);

        if (request.getNomApplication() != null) {
            String nom = request.getNomApplication().trim();
            if (nom.isEmpty()) {
                throw new BusinessException("Le nom de l'application ne peut pas être vide.");
            }
            identite.setNomApplication(limiter(nom, 100, "Le nom de l'application"));
        }
        if (request.getRaisonSociale() != null) {
            identite.setRaisonSociale(vide(limiter(request.getRaisonSociale().trim(), 255, "La raison sociale")));
        }
        if (request.getTypeEtablissement() != null) {
            identite.setTypeEtablissement(request.getTypeEtablissement());
        }
        if (request.getCouleurPrimaire() != null) {
            String couleur = request.getCouleurPrimaire().trim();
            if (!couleur.isEmpty() && !couleur.matches("^#[0-9A-Fa-f]{6}$")) {
                throw new BusinessException("La couleur doit avoir le format #RRGGBB.");
            }
            identite.setCouleurPrimaire(vide(couleur));
        }
        if (request.getAdresse() != null) {
            identite.setAdresse(vide(limiter(request.getAdresse().trim(), 500, "L'adresse")));
        }
        if (request.getTelephone() != null) {
            identite.setTelephone(vide(limiter(request.getTelephone().trim(), 50, "Le téléphone")));
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BusinessException("L'adresse e-mail est invalide.");
            }
            identite.setEmail(vide(limiter(email, 150, "L'e-mail")));
        }
        if (request.getPiedDePage() != null) {
            identite.setPiedDePage(vide(limiter(request.getPiedDePage().trim(), 500, "Le pied de page")));
        }

        if (request.getCircuitInfecteActif() != null) {
            identite.setCircuitInfecteActif(request.getCircuitInfecteActif());
        }
        if (circuitAvant && !profilExploitationService.valeurEffective(identite)) {
            profilExploitationService.verifierDesactivationPossible();
        }

        if (request.getNiveauRattachement() != null) {
            identite.setNiveauRattachement(request.getNiveauRattachement());
        }
        profilExploitationService.verifierChangementNiveau(
                niveauAvant, profilExploitationService.niveauEffectif(identite));

        identite.setDateModification(LocalDateTime.now());
        IdentiteEtablissement enregistree = identiteRepository.save(identite);
        profilExploitationService.invalider();
        return versReponse(enregistree);
    }

    @Transactional
    public IdentiteResponse enregistrerLogo(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Aucun fichier reçu.");
        }
        if (fichier.getSize() > TAILLE_MAX_LOGO) {
            throw new BusinessException("Le logo dépasse 500 Ko.");
        }

        byte[] donnees;
        try {
            donnees = fichier.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Lecture du fichier impossible.");
        }

        String type = detecterType(donnees);
        if (type == null) {
            throw new BusinessException("Format non accepté : utilisez un PNG, un JPEG ou un WebP.");
        }

        IdentiteEtablissement identite = getOuCreer();
        LogoEtablissement ancien = identite.getLogo();

        LogoEtablissement logo = new LogoEtablissement();
        logo.setTypeContenu(type);
        logo.setDonnees(donnees);
        logo.setTaille((long) donnees.length);
        logo.setVersion(System.currentTimeMillis());
        logo.setDateModification(LocalDateTime.now());
        logo = logoRepository.save(logo);

        identite.setLogo(logo);
        identite.setDateModification(LocalDateTime.now());
        identiteRepository.save(identite);

        if (ancien != null) {
            logoRepository.delete(ancien);
        }

        return versReponse(identite);
    }

    @Transactional
    public IdentiteResponse supprimerLogo() {
        IdentiteEtablissement identite = getOuCreer();
        LogoEtablissement logo = identite.getLogo();

        identite.setLogo(null);
        identite.setDateModification(LocalDateTime.now());
        identiteRepository.save(identite);

        if (logo != null) {
            logoRepository.delete(logo);
        }

        return versReponse(identite);
    }

    @Transactional(readOnly = true)
    public Optional<LogoEtablissement> lireLogo() {
        return identiteRepository.findLogo(IdentiteEtablissement.ID_UNIQUE);
    }

    private IdentiteEtablissement getOuCreer() {
        return identiteRepository.findById(IdentiteEtablissement.ID_UNIQUE)
                .orElseGet(() -> identiteRepository.save(new IdentiteEtablissement()));
    }

    private IdentiteResponse versReponse(IdentiteEtablissement identite) {
        Optional<Long> version = identiteRepository.findVersionDuLogo(identite.getId());

        return IdentiteResponse.builder()
                .raisonSociale(identite.getRaisonSociale())
                .nomApplication(identite.getNomApplication())
                .typeEtablissement(identite.getTypeEtablissement())
                .circuitInfecteActif(profilExploitationService.valeurEffective(identite))
                .niveauRattachement(profilExploitationService.niveauEffectif(identite))
                .couleurPrimaire(identite.getCouleurPrimaire())
                .adresse(identite.getAdresse())
                .telephone(identite.getTelephone())
                .email(identite.getEmail())
                .piedDePage(identite.getPiedDePage())
                .logoUrl(version.map(v -> "/api/parametres/logo?v=" + v).orElse(null))
                .logoVersion(version.orElse(null))
                .dateModification(identite.getDateModification())
                .build();
    }

    private String detecterType(byte[] d) {
        if (d.length >= 8
                && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "image/png";
        }
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 12
                && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String limiter(String valeur, int max, String libelle) {
        if (valeur.length() > max) {
            throw new BusinessException(libelle + " dépasse " + max + " caractères.");
        }
        return valeur;
    }

    private String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur;
    }
}
