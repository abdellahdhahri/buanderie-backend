package tn.softsys.buanderie.Sqlserver.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.dtos.ServiceDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.SqlServerServiceService;

import java.util.List;

@RestController("sqlServerServiceController")
@RequestMapping("/api/sqlserver/services")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Service SQL Server", description = "Consultation des services depuis SQL Server")
public class ServiceController {

    private final SqlServerServiceService serviceService;

    @GetMapping
    public List<ServiceDTO> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return serviceService.getAllServices();
    }

    @GetMapping("/{id}")
    public ServiceDTO getServiceById(@PathVariable Long id) {
        return serviceService.getServiceById(id);
    }

    @GetMapping("/ref/{ref}")
    public ServiceDTO getByRef(@PathVariable String ref){
        return serviceService.getByReference(ref);
    }

}
