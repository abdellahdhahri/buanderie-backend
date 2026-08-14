package tn.softsys.buanderie.service.Reporting;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.dto.NotificationDTO;
import tn.softsys.buanderie.entity.Linge;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;


    public void notifierUniformeStocke(Linge linge) {




        NotificationDTO notification = new NotificationDTO(
                "Stock uniforme disponible",
                "Un uniforme propre est disponible en stock"+linge.getRfidTag(),
                "STOCK_UNIFORME",
                LocalDateTime.now(),
                linge.getId(),
                linge.getCodeBarre(),
                linge.getCategorie().name()
        );




        messagingTemplate.convertAndSend(
                "/topic/notifications",
                notification
        );


    }
    public void notifierFinDeVie(Linge linge) {

        NotificationDTO notification = new NotificationDTO(
                "Fin de vie du linge",
                "Le linge " + linge.getCodeBarre()
                        + " a atteint sa durée de vie maximale et doit être remplacé.",
                "FIN_DE_VIE",
                LocalDateTime.now(),
                linge.getId(),
                linge.getCodeBarre(),
                linge.getCategorie().name()
        );

        messagingTemplate.convertAndSend(
                "/topic/notifications",
                notification
        );
    }
}