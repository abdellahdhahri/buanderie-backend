package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.Lot.LotDTO;
import tn.softsys.buanderie.entity.Lot;
import tn.softsys.buanderie.entity.enums.EtatLot;
import tn.softsys.buanderie.service.management.LotService;

import java.util.List;

@RestController
@RequestMapping("/api/lots") @RequiredArgsConstructor
public class LotController {
    private final LotService lotService;
    @GetMapping
    public List<Lot> getAllLots(){
        return  lotService.getAllLots();
    }
    @PostMapping
    public Lot creerLot (LotDTO lot){
        return lotService.creerLot(lot);
    }
//    private LingeDTO toLingeLotDTO(Linge linge) {
//
//        return LingeDTO.builder()
//                .id(linge.getId())
//                .codebarre(linge.getCodeBarre())
//                .categorie(linge.getCategorie().name())
//                .poids(linge.getPoids())
//                .etat(linge.getEtat())
//                .serviceId(
//                        linge.getService() != null
//                                ? linge.getService().getId()
//                                : null
//                )
//                .chambre_id(
//                        linge.getChambreIdSqlServer() != null
//                                ? linge.getChambreIdSqlServer()
//                                : null
//                )
//                .build();
//    }
    @PutMapping("/{codeLot}/vider")
    public ResponseEntity<Lot> viderLot(
            @PathVariable String codeLot) {

        return ResponseEntity.ok(
                lotService.viderLot(codeLot));
    }

@DeleteMapping
public ResponseEntity<String> delete(Long id){
       lotService.deleteLot(id);
       return ResponseEntity.ok("lot supprimé avec succes");
}


    //methode qui retourne list des lot triee
    @GetMapping("/triee")
    public List<Lot> getLotTriee(EtatLot etatLot){
        return lotService.findLotTriee(EtatLot.EN_TRI);
    }
}
