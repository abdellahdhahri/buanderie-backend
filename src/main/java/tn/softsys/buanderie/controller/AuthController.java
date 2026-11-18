package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.LoginRequestDTO;
import tn.softsys.buanderie.dto.LoginResponseDTO;
import tn.softsys.buanderie.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {

        LoginResponseDTO response = authService.login(
                request.getLogin(),
                request.getPassword()
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(java.security.Principal principal) {
        if (principal != null) {
            authService.logout(principal.getName());
        }
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/login/badge")
    public ResponseEntity<LoginResponseDTO> loginByBadge(
            @RequestParam String codeBarre) {

        LoginResponseDTO response =
                authService.loginByBadge(codeBarre);

        return ResponseEntity.ok(response);
    }
}
