package projekt.pk.edu.pl.bookstore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projekt.pk.edu.pl.bookstore.dto.book.BookRequest;
import projekt.pk.edu.pl.bookstore.dto.book.BookResponse;
import projekt.pk.edu.pl.bookstore.service.BookService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "Operacje związane z katalogiem książek.")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "Lista książek", description = "Zwraca stronicowaną listę książek z opcjonalnym filtrem tekstowym.")
    @ApiResponse(responseCode = "200", description = "Lista książek została zwrócona.")
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @Parameter(description = "Fraza wyszukiwania po tytule, autorze lub ISBN.", example = "Clean Code")
            @RequestParam(required = false) String search,
            @Parameter(description = "Numer strony.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Rozmiar strony.", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Pole sortowania.", example = "title")
            @RequestParam(defaultValue = "title") String sortBy,
            @Parameter(description = "Kierunek sortowania.", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(bookService.getAllBooks(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły książki", description = "Zwraca szczegóły pojedynczej książki.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Szczegóły książki."),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono książki.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(
            summary = "Dodanie książki",
            description = "Tworzy nową książkę w katalogu. Endpoint dostępny dla administratora.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Książka została utworzona."),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane książki.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak tokenu.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dane nowej książki.",
            content = @Content(
                    schema = @Schema(implementation = BookRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "isbn": "9780132350884",
                              "title": "Clean Code",
                              "author": "Robert C. Martin",
                              "publisher": "Prentice Hall",
                              "publicationYear": 2008,
                              "totalCopies": 5
                            }
                            """)
            )
    )
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(
            summary = "Aktualizacja książki",
            description = "Aktualizuje dane książki. Endpoint dostępny dla administratora.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Książka została zaktualizowana."),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono książki.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Konflikt danych książki.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Nowe dane książki.",
            content = @Content(
                    schema = @Schema(implementation = BookRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "isbn": "9780132350884",
                              "title": "Clean Code",
                              "author": "Robert C. Martin",
                              "publisher": "Prentice Hall",
                              "publicationYear": 2008,
                              "totalCopies": 7
                            }
                            """)
            )
    )
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Usunięcie książki",
            description = "Usuwa książkę z katalogu, jeśli żaden egzemplarz nie jest w obiegu.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Książka została usunięta."),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono książki.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Książki nie można usunąć z powodu egzemplarzy w obiegu.", content = @Content(schema = @Schema(implementation = projekt.pk.edu.pl.bookstore.dto.auth.ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}