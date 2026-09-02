package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerPersonnelRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;
import tn.softsys.buanderie.dto.Reception.ReceptionRFIDRequest;
import tn.softsys.buanderie.dto.Reception.ReceptionRequest;
import tn.softsys.buanderie.entity.Agent;
import tn.softsys.buanderie.repository.AgentRepository;
import tn.softsys.buanderie.service.Process.ReceptionService;
import tn.softsys.buanderie.service.Process.ReceptionService.ReceptionResultDTO;

@RestController
@RequestMapping("/api/reception")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService service;
    private final AgentRepository agentRepository;
    private final  SqlServerPersonnelRepository personnelRepo;

    @PostMapping
    public ResponseEntity<ReceptionResultDTO> recevoir(
            @RequestBody ReceptionRequest request,
            @RequestParam String matricule) {

        Personnel agent = personnelRepo.findByCodeBarre(matricule);
//                .orElseThrow(() -> new RuntimeException("Agent introuvable : " + matricule));

        ReceptionResultDTO result = service.recevoirPlusieurs(request, matricule);
        return ResponseEntity.ok(result);
    }

   /* @PostMapping("/receptionRfid")
    public ResponseEntity<ReceptionService.ReceptionRFIDResultDTO> recevoirRfid(@RequestBody ReceptionRFIDRequest request
                                                                               ){
        Agent agent = agentRepository.findByMatricule(request.getMatriculeAgent())
                .orElseThrow(() -> new RuntimeException("Agent introuvable : " + request.getMatriculeAgent()));
        ReceptionService.ReceptionRFIDResultDTO result = service.recevoirRFID(request, agent);
        return ResponseEntity.ok(result);

    }
*/

   /* @PostMapping("/receptionne-rfid")
    public ResponseEntity<String> scannerRFID(@RequestBody RFIDScanRequest request) {
        service.ReceptionneRfid(request.getRfidTags());
        return ResponseEntity.ok("reception effectuer avec succes");
    }
*/
}
