package tn.softsys.buanderie.controller;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.LingeImportRepository;
import tn.softsys.buanderie.Sqlserver.projection.LingeImportableProjection;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.LingeImportService;
import tn.softsys.buanderie.dto.Linge.AssocierRfidRequest;
import tn.softsys.buanderie.dto.Historique.HistoriqueLingeDto;
import tn.softsys.buanderie.dto.Linge.LingeImportRequest;
import tn.softsys.buanderie.dto.LingeDTO;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.service.Process.CollecteServiceService;
import tn.softsys.buanderie.service.management.LingeService;
@RestController @RequestMapping("/api/linges") @RequiredArgsConstructor
public class LingeController {
    private final LingeService lingeService;
    private final CollecteServiceService collecteService;
    private final LingeImportRepository lingeImportRepository;
    private final LingeImportService lingeImportService;


   // @PreAuthorize("hasAuthority('LIG1305')")
    @Operation(summary = "Récupérer les linges avec pagination et filtres")
    @GetMapping("/paged")
    public Page<LingeDTO> getLingesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String etat,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String search) {
        return lingeService.getLingesPaged(page, size, categorie, etat, serviceId, search);
    }
    @GetMapping public List<Linge> liste() { return lingeService.findAll(); }
    @Operation(summary = "Récupérer l historique global")

    @GetMapping("/historique/historique-global")
    public List<HistoriqueLinge> getAllHistorique() {

        return lingeService.getHistorique();
    }
    @GetMapping("/{id}") public Linge getById(@PathVariable Long id) { return lingeService.findById(id); }
    @GetMapping("/rfid/{rfid}") public Linge getByrfid(@PathVariable String rfid) { return lingeService.getByRfidTag(rfid); }
    @GetMapping("/bac/{bacId}") public List<Linge> getByBac(@PathVariable Long bacId) { return lingeService.findByBac(bacId); }
    @GetMapping("/barcode/{code}") public Linge getByBarcode(@PathVariable String code) { return lingeService.findByCodeBarre(code); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Linge creer(@Valid @RequestBody LingeDTO linge) { return lingeService.creer(linge); }
    @PatchMapping("/{id}/etat")
    public Linge changerEtat(@PathVariable Long id, @RequestBody ChangerEtatRequest req) {
        return lingeService.changerEtat(id, req.getEtat(), req.getMatriculeAgent(), req.getObservations());
    }

    @Operation(summary = "affecter une linge a un bac")
    @PostMapping("/{bacId}/ajouter-linge")
    public ResponseEntity<Linge> ajouterLinge(
            @PathVariable Long bacId,
            @RequestParam String codeBarre) {

        return ResponseEntity.ok(
                lingeService.affecterAuBac(codeBarre, bacId)
        );
    }
    @Operation(summary = "consulter l historique d 'une linge a partir de son code a barre ")
    @GetMapping("/historique/code/{codeBarre}")
    public List<HistoriqueLingeDto> historiqueParCodeBarre(
            @PathVariable String codeBarre) {

        return lingeService.getHistoriqueLingeByCodeBarre(codeBarre);
    }
 /*   @GetMapping("/Linge/origin/{id}")
    public Collecte getOrigin(@PathVariable Long id) {

        return lingeService.getLingeInfo(id);
    }*/
 @Operation(summary = "Récupérer les linges d'un lot")

    @GetMapping("/lots/{codeLot}/linges")
    public List<Linge> getLingesByLot(
            @PathVariable String codeLot) {

        return lingeService.getLingesByLotCode(codeLot);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Linge> update(
            @PathVariable Long id,
            @Valid @RequestBody LingeDTO dto) {

        System.out.println("📥 Mise à jour du linge ID: " + id);
        System.out.println("   - Catégorie: " + dto.getCategorie());
        System.out.println("   - ServiceId: " + dto.getServiceId());
        System.out.println("   - PorteurId: " + dto.getPorteurId());
        System.out.println("   - Durée de vie: " + dto.getDurevie());
        System.out.println("   - État: " + dto.getEtat());

        return ResponseEntity.ok(lingeService.update(id, dto));
    }

    @PostMapping("/associer-rfid")
    public ResponseEntity<Linge> associerRfid(
            @RequestBody AssocierRfidRequest request) {

        return ResponseEntity.ok(
                lingeService.associerTagRfid(request));
    }

    @GetMapping("/a-importer")
    public Page<LingeImportableProjection> getLingesAImporter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").ascending()
        );

        return lingeImportRepository.findLingesImportables(pageable);
    }
    @PostMapping("/import")
    public ResponseEntity<?> importerLinges(
            @RequestBody List<LingeImportRequest> requests) {

        try {

            List<Linge> linges =
                    lingeImportService.importerLinges(requests);

            return ResponseEntity.ok(linges);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Erreur import linges : "
                                    + e.getMessage()
                    );

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Erreur interne : "
                                    + e.getMessage()
                    );
        }
    }

    @Data public static class ChangerEtatRequest { private EtatLinge etat; private String matriculeAgent; private String observations; }
}
