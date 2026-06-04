package projekt.pk.edu.pl.bookstore.controller;

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
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<BorrowResponse> borrowBook(
            @Valid @RequestBody BorrowRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(borrowService.borrowBook(request, authentication.getName()));
    }

    @PostMapping("/{borrowId}/return")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<BorrowResponse> returnBook(
            @PathVariable Long borrowId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(borrowService.returnBook(borrowId, authentication.getName()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<List<BorrowResponse>> getCurrentUserHistory(Authentication authentication) {
        return ResponseEntity.ok(borrowService.getCurrentUserHistory(authentication.getName()));
    }
}
