package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.softsys.buanderie.service.Process.StockageService;

@RestController
@RequestMapping("/api/stockage")
@RequiredArgsConstructor

public class StockageController {

    private final StockageService stockageService;



}
