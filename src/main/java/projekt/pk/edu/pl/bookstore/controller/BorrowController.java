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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projekt.pk.edu.pl.bookstore.dto.borrow.BorrowRequest;
import projekt.pk.edu.pl.bookstore.dto.borrow.BorrowResponse;
import projekt.pk.edu.pl.bookstore.service.BorrowService;

import java.util.List;

@RestController
@RequestMapping("/api/borrows")
@Tag(name = "Borrows", description = "Wypożyczenia, zwroty i historia wypożyczeń użytkownika.")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(
            summary = "Realizacja wypożyczenia przez bibliotekarza",
            description = "Bibliotekarz wydaje książkę na podstawie aktywnej rezerwacji czytelnika.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Wypożyczenie zostało utworzone."),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane żądania.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak lub nieprawidłowy token.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień bibliotekarza.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono rezerwacji lub książki.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Konflikt biznesowy wypożyczenia.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dane potrzebne do realizacji wypożyczenia.",
            content = @Content(
                    schema = @Schema(implementation = BorrowRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "reservationId": 10
                            }
                            """)
            )
    )
    public ResponseEntity<BorrowResponse> borrowBook(
            @Valid @RequestBody BorrowRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(borrowService.borrowBook(request));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(
            summary = "Aktywne wypożyczenia",
            description = "Zwraca listę wszystkich aktualnie wypożyczonych książek (status BORROWED).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista aktywnych wypożyczeń."),
            @ApiResponse(responseCode = "401", description = "Brak lub nieprawidłowy token.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień bibliotekarza.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    public ResponseEntity<List<BorrowResponse>> getActiveBorrows() {
        return ResponseEntity.ok(borrowService.getAllActiveBorrows());
    }

    @PostMapping("/{borrowId}/return")
    @PreAuthorize("hasRole('READER')")
    @Operation(
            summary = "Zwrot książki",
            description = "Oznacza wypożyczenie jako zwrócone i zwiększa liczbę dostępnych egzemplarzy.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zwrot został zarejestrowany."),
            @ApiResponse(responseCode = "401", description = "Brak lub nieprawidłowy token.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Wypożyczenie nie należy do użytkownika.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono wypożyczenia.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Konflikt biznesowy zwrotu.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    public ResponseEntity<BorrowResponse> returnBook(
            @PathVariable Long borrowId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(borrowService.returnBook(borrowId, authentication.getName()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('READER')")
    @Operation(
            summary = "Historia wypożyczeń użytkownika",
            description = "Zwraca historię wypożyczeń aktualnie zalogowanego czytelnika wraz ze statusem i datami.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historia wypożyczeń użytkownika."),
            @ApiResponse(responseCode = "401", description = "Brak lub nieprawidłowy token.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    public ResponseEntity<List<BorrowResponse>> getCurrentUserHistory(Authentication authentication) {
        return ResponseEntity.ok(borrowService.getCurrentUserHistory(authentication.getName()));
    }
}
