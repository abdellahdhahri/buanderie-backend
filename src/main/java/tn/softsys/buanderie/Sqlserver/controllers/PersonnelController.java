package tn.softsys.buanderie.Sqlserver.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.softsys.buanderie.Sqlserver.dtos.PersonnelDto;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixPersonnelService;


import java.util.List;

@RestController("sqlServerPersonnelController")
@RequestMapping("/api/sqlserver/personnel")
@RequiredArgsConstructor
@Tag(name = "personnel SQL Server", description = "Consultation des personnels depuis SQL Server")
public class PersonnelController {
    private final CubixPersonnelService cubixPersonnelService;


    @GetMapping
    public List<PersonnelDto>getAll(){
        return cubixPersonnelService.getAll();
    }

    @GetMapping("code/{code}")
    public PersonnelDto getByCode(@PathVariable String code){
        return cubixPersonnelService.getByCode(code);
    }
    @GetMapping("/{id}")
    public PersonnelDto getById(@PathVariable Long id) {
        return cubixPersonnelService.getById(id);
    }

}
