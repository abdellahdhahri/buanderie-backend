package tn.softsys.buanderie.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.ReclamationRequest;
import tn.softsys.buanderie.entity.Reclamation;
import tn.softsys.buanderie.service.Process.ReclamationService;
import java.util.List;
@RestController @RequestMapping("/api/reclamations") @RequiredArgsConstructor
public class ReclamationController {
    private final ReclamationService reclamationService;


    @GetMapping
    public List<Reclamation> reclamations (){
        return reclamationService.getAll();
    }
    @PostMapping
    public ResponseEntity<Reclamation> creer(
            @RequestBody ReclamationRequest request) {

        Reclamation reclamation =
                reclamationService.creer(request);

        return ResponseEntity.ok(reclamation);
    }
}
