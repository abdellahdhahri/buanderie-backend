package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.softsys.buanderie.dto.Collectes.ResumeCollecteDTO;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.repository.CollecteRepository;
import tn.softsys.buanderie.service.management.CollecteResumeService;

import java.util.List;

@RestController
@RequestMapping("/api/collecte") @RequiredArgsConstructor
public class CollecteController {

    private final CollecteResumeService collecteResumeService;
    private  final CollecteRepository collecteRepository;


    @GetMapping
    public List<Collecte> getResume(){

        return collecteRepository.findAll();

    }
    @GetMapping("/{id}/resume")
    public ResumeCollecteDTO getResume(@PathVariable Long id){

        return collecteResumeService.getResume(id);

    }
}
