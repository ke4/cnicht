package co.uk.karel.dev.cnicht.auth.controller;

import co.uk.karel.dev.cnicht.auth.entity.UserAccount;
import co.uk.karel.dev.cnicht.auth.service.EmailAlreadyExistsException;
import co.uk.karel.dev.cnicht.auth.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestParam String email,
                                                         @RequestParam String password) {
        UserAccount account = registrationService.register(email, password);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("email", account.getEmail()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
