package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RetourRemiseService {

    private final RetourRemiseRepository retourRemiseRepository;
    private final RetourRemiseItemRepository retourRemiseItemRepository;
    private final RemiseLingeRepository remiseLingeRepository;
    private final LingeRepository lingeRepository;
    private final AgentRepository agentRepository;
    private final RemiseLingeItemRepository remiseLingeItemRepository;

//    public RetourRemiseItem scannerRetour(
//            Long remiseLingeId,
//            String code,
//            Long agentId
//    ) {
//
//        RemiseLinge remiseLinge =
//                remiseLingeRepository.findById(remiseLingeId)
//                        .orElseThrow(() ->
//                                new RuntimeException(
//                                        "Remise introuvable : " + remiseLingeId
//                                )
//                        );
//
//        Agent agent =
//                agentRepository.findById(agentId)
//                        .orElseThrow(() ->
//                                new RuntimeException(
//                                        "Agent introuvable : " + agentId
//                                )
//                        );
//
//
//
//        Linge linge = lingeRepository.findByCodeBarre(code)
//                .orElseGet(() ->
//                        lingeRepository.findByRfidTag(code)
////                                .orElseThrow(() ->
////                                        new RuntimeException(
////                                                "Aucun linge trouvé avec le code : "
////                                                        + code
////                                        )
////                                )
//                );
//
//
//
//        boolean appartient =
//                remiseLingeItemRepository
//                        .existsByRemiseLinge_IdAndLinge_Id(
//                                remiseLingeId,
//                                linge.getId()
//                        );
//
//        if (!appartient) {
//
//            throw new RuntimeException(
//                    "Ce linge ne fait pas partie de cette remise."
//            );
//        }
//        boolean dejaRetourne =
//                retourRemiseItemRepository
//                        .existsByLinge_IdAndRetourRemise_RemiseLinge_Id(
//                                linge.getId(),
//                                remiseLingeId
//                        );
//
//        if (dejaRetourne) {
//
//            throw new RuntimeException(
//                    "Ce linge a déjà été déclaré comme retour pour cette remise."
//            );
//        }
//
//        List<RetourRemise> retours =
//                retourRemiseRepository
//                        .findByRemiseLinge_IdOrderByDateRetourDesc(
//                                remiseLingeId
//                        );
//
//        RetourRemise retour;
//
//        if (retours.isEmpty()) {
//
//            retour = RetourRemise.builder()
//                    .remiseLinge(remiseLinge)
//                    .agent(agent)
//                    .build();
//
//            retour = retourRemiseRepository.save(retour);
//
//        } else {
//
//            // On utilise le retour existant
//            retour = retours.get(0);
//        }
//
//        RetourRemiseItem item =
//                RetourRemiseItem.builder()
//                        .retourRemise(retour)
//                        .linge(linge)
//                        .build();
//
//        return retourRemiseItemRepository.save(item);
//    }
}