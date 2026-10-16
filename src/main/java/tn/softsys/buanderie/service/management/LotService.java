package tn.softsys.buanderie.service.management;


import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.dto.LingeDTO;
import tn.softsys.buanderie.dto.Lot.LotDTO;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.EtatLot;
import tn.softsys.buanderie.entity.Lot;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.LotRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class LotService {
    private final LotRepository lotRepository;
    private final LingeRepository lingeRepository;
    private static final Map<CategLinge, String> ABREVIATIONS = new EnumMap<>(CategLinge.class);

    static {
        ABREVIATIONS.put(CategLinge.DRAP, "DRP");
        ABREVIATIONS.put(CategLinge.TAIE_OREILLER, "TAI");
        ABREVIATIONS.put(CategLinge.COUETTE, "COU");
        ABREVIATIONS.put(CategLinge.CHAMP_OPERATOIRE, "CHO");
        ABREVIATIONS.put(CategLinge.SERVIETTE_BAIN, "SVB");
        ABREVIATIONS.put(CategLinge.SERVIETTE_VISAGE, "SVV");
        ABREVIATIONS.put(CategLinge.SERVIETTE_TABLE, "SVT");
        ABREVIATIONS.put(CategLinge.PEIGNOIR, "PEI");
        ABREVIATIONS.put(CategLinge.NAPPE, "NAP");
        ABREVIATIONS.put(CategLinge.UNIFORME, "UNI");
        ABREVIATIONS.put(CategLinge.AUTRE, "AUT");
    }
    public LotService(LotRepository lotRepository, LingeRepository lingeRepository) {
        this.lotRepository = lotRepository;
        this.lingeRepository = lingeRepository;
    }


    public List<Lot> getAllLots(){
        return  lotRepository.findAll();
    }
    public Lot creerLot(LotDTO dto) {

        Lot lot = new Lot();

        lot.setTypeCollecte(dto.getTypeCollecte());
        lot.setCategorie(dto.getCategorie());
        lot.setCapaciteMax(dto.getCapaciteMax());
        lot.setPoidsActuel(0.0);
        lot.setEtat(EtatLot.EN_TRI);
        lot.setDateCreation(LocalDateTime.now());

        lot.setCodeLot(genererCodeLot(dto));

        return lotRepository.save(lot);
    }
    public List<LingeDTO> getLingesByCodeLot(String codeLot) {

        Lot lot = lotRepository.findByCodeLot(codeLot)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lot", "codeLot", codeLot));

        return lingeRepository.findByLot_CodeLot(codeLot)
                .stream()
                .map(this::toLingeLotDTO)
                .toList();
    }



    private LingeDTO toLingeLotDTO(Linge linge) {

        return LingeDTO.builder()
                .id(linge.getId())
                .codebarre(linge.getCodeBarre())
                .categorie(linge.getCategorie().name())
                .poids(linge.getPoids())
                .etat(linge.getEtat())
                .serviceId(
                        linge.getServiceId()
                )
                .chambre_id(
                        linge.getChambreIdSqlServer() != null
                                ? linge.getChambreIdSqlServer()
                                : null
                )
                .build();
    }



    @Transactional
    public Lot viderLot(String codeLot) {


        Lot lot = lotRepository.findByCodeLot(codeLot)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lot", "id", codeLot));

        List<Linge> linges = lingeRepository.findByLot_CodeLot(codeLot);

        for (Linge linge : linges) {

            EtatLinge ancienEtat = linge.getEtat();

            linge.setLot(null);


            lingeRepository.save(linge);


        }

        lot.setPoidsActuel(0.0);
        lot.setEtat(EtatLot.EN_TRI);

        return lotRepository.save(lot);
    }


    public void deleteLot(Long id){
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("lot introuvable avec l'id : " + id));
        lotRepository.delete(lot);
    }
    public List<Lot> findLotTriee(EtatLot etatLot){
        return lotRepository.findByEtat(etatLot);
    }
    private String genererCodeLot(LotDTO dto) {
        String type = dto.getTypeCollecte().name().substring(0, 2).toUpperCase();

        String nomCategorie = dto.getCategorie().name();
        String categorie = ABREVIATIONS.getOrDefault(
                dto.getCategorie(),
                nomCategorie.substring(0, Math.min(3, nomCategorie.length()))
        );

        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMdd"));

        String prefixe = type + "-" + categorie + "-" + date + "-";

        int sequence = 1;
        String code = prefixe + String.format("%02d", sequence);

        while (lotRepository.existsByCodeLot(code)) {
            sequence++;
            code = prefixe + String.format("%02d", sequence);
        }

        return code;
    }
}
