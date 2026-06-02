package projekt.pk.edu.pl.bookstore.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginResponse;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationResponse;
import projekt.pk.edu.pl.bookstore.service.AuthenticationService;
import projekt.pk.edu.pl.bookstore.service.RegistrationService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;

    public AuthController(RegistrationService registrationService, AuthenticationService authenticationService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }
}
