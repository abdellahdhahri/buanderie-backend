package tn.softsys.buanderie.Chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {

    @NotBlank(message = "La question est obligatoire")
    private String question;
}