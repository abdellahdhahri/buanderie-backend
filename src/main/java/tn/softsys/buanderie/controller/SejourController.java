    package tn.softsys.buanderie.controller;

    import jakarta.validation.Valid;
    import lombok.Data;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.HttpStatus;
    import org.springframework.web.bind.annotation.*;
    import tn.softsys.buanderie.entity.Sejour;
    import tn.softsys.buanderie.service.management.SejourService;

    import java.time.LocalDate;
    import java.util.List;

    @RestController
    @RequestMapping("/sejours")
    @RequiredArgsConstructor
    public class SejourController {
        private final SejourService sejourService;

        @GetMapping
        public List<Sejour> liste() {
            return sejourService.findAll();
        }

        @GetMapping("/{id}")
        public Sejour getById(@PathVariable Long id) {
            return sejourService.findById(id);
        }

        @GetMapping("/chambre/{chambreId}")
        public List<Sejour> getByChambre(@PathVariable Long chambreId) {
            return sejourService.findByChambre(chambreId);
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public Sejour creer(@Valid @RequestBody SejourRequest req) {
            Sejour s = Sejour.builder().nomClient(req.getNomClient()).dateArrivee(req.getDateArrivee()).dateDepart(req.getDateDepart()).build();
            return sejourService.creer(s, req.getChambreId());
        }

        @DeleteMapping("/delete/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void supprimer(@PathVariable Long id) {
            sejourService.supprimer(id);
        }

        @Data
        static class SejourRequest {
            private String nomClient;
            private LocalDate dateArrivee;
            private LocalDate dateDepart;
            private Long chambreId;
        }
    }
