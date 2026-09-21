package tn.softsys.buanderie.Sqlserver.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import tn.softsys.buanderie.Sqlserver.Cubix.CubixChambreCacheService;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixChambreService;
import tn.softsys.buanderie.dto.ChambreDto;

@RestController("sqlServerChambreController")
@RequestMapping("/api/sqlserver/chambres")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(
        name = "Chambre SQL Server",
        description = "Consultation des chambres depuis Cubix"
)
public class ChambreController {

    private final CubixChambreService chambreService;
    private final CubixChambreCacheService cubixChambreCacheService;



    @GetMapping
    public Page<ChambreDto> getAllChambres(Pageable pageable) {
        return chambreService.getAll(pageable);
    }


//    @GetMapping("/{id}")
//    public ChambreDto getChambreById(
//            @PathVariable Long id
//    ) {
//        return chambreService.getById(id);
//    }



    @GetMapping("/code/{code}")
    public ChambreDto getChambreByCodeBarre(
            @PathVariable("code") String code
    ) {
        return chambreService.getByCodeBarre(code);
    }


//    @GetMapping("/service/{serviceId}")
//    public java.util.List<ChambreDto> getChambresByServiceId(
//            @PathVariable Long serviceId
//    ) {
//        return chambreService.getByServiceId(serviceId);
//    }
}