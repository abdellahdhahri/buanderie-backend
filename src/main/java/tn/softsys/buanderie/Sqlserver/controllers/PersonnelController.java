package tn.softsys.buanderie.Sqlserver.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.softsys.buanderie.Sqlserver.dtos.PersonnelDto;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.PersonnelService;


import java.util.List;
import java.util.Optional;

@RestController("sqlServerPersonnelController")
@RequestMapping("/api/sqlserver/personnel")
@RequiredArgsConstructor
@Tag(name = "personnel SQL Server", description = "Consultation des personnels depuis SQL Server")
public class PersonnelController {
    private final PersonnelService personnelService;


    @GetMapping
    public List<Personnel>getAll(){
        return personnelService.getAll();
    }

    @GetMapping("code/{code}")
    public PersonnelDto getByCode(@PathVariable String code){
        return personnelService.getByCode(code);
    }
    @GetMapping("/{id}")
    public Optional<Personnel> getById(@PathVariable Long id) {
        return personnelService.getById(id);
    }

}
