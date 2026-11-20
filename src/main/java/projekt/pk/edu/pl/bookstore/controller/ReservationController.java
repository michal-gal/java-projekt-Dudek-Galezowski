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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projekt.pk.edu.pl.bookstore.dto.reservation.CreateReservationRequest;
import projekt.pk.edu.pl.bookstore.dto.reservation.ReservationResponse;
import projekt.pk.edu.pl.bookstore.service.ReservationService;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Tworzenie rezerwacji książek przez czytelnika.")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    @Operation(summary = "Moje rezerwacje", description = "Zwraca listę rezerwacji zalogowanego czytelnika.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Lista rezerwacji.")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(Authentication authentication) {
        return ResponseEntity.ok(reservationService.getMyReservations(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('READER')")
    @Operation(summary = "Anulowanie rezerwacji", description = "Anuluje aktywną rezerwację należącą do zalogowanego czytelnika.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rezerwacja anulowana."),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono rezerwacji.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Rezerwacja nie jest aktywna.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id, Authentication authentication) {
        reservationService.cancelReservation(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Aktywne rezerwacje", description = "Zwraca listę wszystkich aktywnych rezerwacji (dla bibliotekarza).",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Lista aktywnych rezerwacji.")
    public ResponseEntity<List<ReservationResponse>> getActiveReservations() {
        return ResponseEntity.ok(reservationService.getAllActiveReservations());
    }

    @PostMapping
    @PreAuthorize("hasRole('READER')")
    @Operation(
            summary = "Utworzenie rezerwacji",
            description = "Tworzy rezerwację książki dla zalogowanego czytelnika, jeśli egzemplarze są dostępne.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rezerwacja została utworzona."),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane żądania.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak lub nieprawidłowy token.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono książki.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Konflikt biznesowy rezerwacji.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dane potrzebne do utworzenia rezerwacji.",
            content = @Content(
                    schema = @Schema(implementation = CreateReservationRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "bookId": 1
                            }
                            """)
            )
    )
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request, authentication.getName()));
    }
}