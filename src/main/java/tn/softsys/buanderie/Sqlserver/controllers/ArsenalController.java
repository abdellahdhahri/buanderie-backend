package tn.softsys.buanderie.Sqlserver.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.softsys.buanderie.Sqlserver.dtos.ArsenalDto;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.SqlServerArsenalService;


import java.util.List;

@RestController("sqlServerArsenalController")
@RequestMapping("/api/sqlserver/arsenal")
@RequiredArgsConstructor
@Tag(name = "Arsenal SQL Server", description = "Consultation des Arsenals depuis SQL Server")

public class ArsenalController {
    private final SqlServerArsenalService arsenalService;




    @GetMapping
    public List<ArsenalDto> getall(){
        return arsenalService.getAll();
    }




}
