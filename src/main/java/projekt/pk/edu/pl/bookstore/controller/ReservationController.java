package projekt.pk.edu.pl.bookstore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> createReservation() {
        return ResponseEntity.ok("Zalogowany jako USER: Rezerwacja przyjęta.");
    }
}