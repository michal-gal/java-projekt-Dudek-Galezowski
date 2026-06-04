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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projekt.pk.edu.pl.bookstore.dto.reservation.CreateReservationRequest;
import projekt.pk.edu.pl.bookstore.dto.reservation.ReservationResponse;
import projekt.pk.edu.pl.bookstore.service.ReservationService;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Tworzenie rezerwacji książek przez czytelnika.")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
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