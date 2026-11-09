package tn.softsys.buanderie.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.IdentiteRequest;
import tn.softsys.buanderie.dto.IdentiteResponse;
import tn.softsys.buanderie.entity.LogoEtablissement;
import tn.softsys.buanderie.service.management.IdentiteEtablissementService;

@RestController
@RequestMapping("/api/parametres")
@RequiredArgsConstructor
@Tag(name = "Identité de l'établissement", description = "Nom, logo et coordonnées affichés dans l'application")
public class IdentiteEtablissementController {

    private final IdentiteEtablissementService identiteService;

    @GetMapping("/identite")
    public IdentiteResponse lire() {
        return identiteService.lire();
    }

    @PutMapping("/identite")
    public IdentiteResponse modifier(@RequestBody IdentiteRequest request) {
        return identiteService.modifier(request);
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        LogoEtablissement logo = identiteService.lireLogo().orElse(null);
        if (logo == null) {
            return ResponseEntity.notFound().build();
        }

        String etag = "\"" + logo.getVersion() + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)).cachePublic())
                .contentType(MediaType.parseMediaType(logo.getTypeContenu()))
                .body(logo.getDonnees());
    }

    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IdentiteResponse enregistrerLogo(@RequestParam("fichier") MultipartFile fichier) {
        return identiteService.enregistrerLogo(fichier);
    }

    @DeleteMapping("/logo")
    public IdentiteResponse supprimerLogo() {
        return identiteService.supprimerLogo();
    }
}
