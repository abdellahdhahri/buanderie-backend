package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.PersonnelLingeEtape;
import tn.softsys.buanderie.service.management.PersonnelLingeEtapeService;

import java.util.List;

@RestController
@RequestMapping("/api/personnel-linge-etapes")
@RequiredArgsConstructor
public class PersonnelLingeEtapeController {

    private final PersonnelLingeEtapeService service;




    @GetMapping("/linge-etape/{lingeEtapeId}/personnels")
    public ResponseEntity<List<Personnel>> getPersonnelByLingeEtapeId(
            @PathVariable Long lingeEtapeId
    ) {

        return ResponseEntity.ok(
                service.getPersonnelByLingeEtapeId(lingeEtapeId)
        );
    }




    @GetMapping
    public ResponseEntity<List<PersonnelLingeEtape>> getAll() {

        return ResponseEntity.ok(
                service.getAll()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id
    ) {

        try {

            return ResponseEntity.ok(
                    service.getById(id)
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody PersonnelLingeEtape data
    ) {

        try {

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(service.create(data));

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody PersonnelLingeEtape data
    ) {

        try {

            return ResponseEntity.ok(
                    service.update(id, data)
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id
    ) {

        try {

            service.delete(id);

            return ResponseEntity.ok(
                    "Association supprimée avec succès"
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> exists(
            @RequestParam Long personnelId,
            @RequestParam Long lingeEtapeId
    ) {

        return ResponseEntity.ok(
                service.exists(
                        personnelId,
                        lingeEtapeId
                )
        );
    }
}