package projekt.pk.edu.pl.bookstore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projekt.pk.edu.pl.bookstore.dto.auth.CurrentUserResponse;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginResponse;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationResponse;
import projekt.pk.edu.pl.bookstore.service.AuthenticationService;
import projekt.pk.edu.pl.bookstore.service.RegistrationService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Rejestracja, logowanie i pobieranie danych zalogowanego użytkownika.")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;

    public AuthController(RegistrationService registrationService, AuthenticationService authenticationService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Rejestracja użytkownika", description = "Tworzy nowego użytkownika z domyślną rolą READER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Użytkownik został utworzony."),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane rejestracji.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email jest już zajęty.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dane rejestracji nowego użytkownika.",
            content = @Content(
                    schema = @Schema(implementation = RegistrationRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "firstName": "Jan",
                              "lastName": "Kowalski",
                              "email": "jan.kowalski@example.com",
                              "password": "superhaslo123"
                            }
                            """)
            )
    )
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Logowanie użytkownika", description = "Zwraca token JWT potrzebny do autoryzacji kolejnych żądań.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logowanie zakończone sukcesem."),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane logowania.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Błędny email lub hasło.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dane logowania użytkownika.",
            content = @Content(
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "email": "jan.kowalski@example.com",
                              "password": "superhaslo123"
                            }
                            """)
            )
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Dane bieżącego użytkownika",
            description = "Zwraca profil aktualnie zalogowanego użytkownika na podstawie tokenu JWT.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil użytkownika."),
            @ApiResponse(responseCode = "401", description = "Brak lub nieprawidłowy token.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authenticationService.getCurrentUser(authentication.getName()));
    }
}
