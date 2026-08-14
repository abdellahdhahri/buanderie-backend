package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.softsys.buanderie.dto.PliageRequest;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.service.Process.PliageService;
@RestController
@RequestMapping("/api/pliage")
@RequiredArgsConstructor
public class PliageController {


    private final PliageService pliageService;


    @PostMapping
    public ResponseEntity<Linge>plier(@RequestBody
    PliageRequest request){
        return ResponseEntity.ok(pliageService.plier(request));

    }
}
