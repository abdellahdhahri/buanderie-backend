package tn.softsys.buanderie.Chatbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Chatbot.dto.AiChatRequest;
import tn.softsys.buanderie.Chatbot.dto.AiChatResponse;
import tn.softsys.buanderie.Chatbot.service.AiAssistantService;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/assistant")
    public ResponseEntity<AiChatResponse> askAssistant(
            @Valid @RequestBody AiChatRequest request
    ) {

        AiChatResponse response =
                aiAssistantService.ask(request.getQuestion());

        return ResponseEntity.ok(response);
    }
}