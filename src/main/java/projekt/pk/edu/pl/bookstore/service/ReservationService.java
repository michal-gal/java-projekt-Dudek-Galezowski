package projekt.pk.edu.pl.bookstore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projekt.pk.edu.pl.bookstore.dto.reservation.CreateReservationRequest;
import projekt.pk.edu.pl.bookstore.dto.reservation.ReservationResponse;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.entity.Reservation;
import projekt.pk.edu.pl.bookstore.entity.ReservationStatus;
import projekt.pk.edu.pl.bookstore.entity.User;
import projekt.pk.edu.pl.bookstore.exception.ActiveReservationExistsException;
import projekt.pk.edu.pl.bookstore.exception.BookUnavailableException;
import projekt.pk.edu.pl.bookstore.exception.ResourceNotFoundException;
import projekt.pk.edu.pl.bookstore.repository.BookRepository;
import projekt.pk.edu.pl.bookstore.repository.ReservationRepository;
import projekt.pk.edu.pl.bookstore.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class ReservationService {

    private static final long RESERVATION_DURATION_DAYS = 3;

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            BookRepository bookRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika o emailu: " + normalizedEmail));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono książki o ID: " + request.bookId()));

        if (book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException(book.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        if (reservationRepository.existsActiveReservation(user.getId(), book.getId(), ReservationStatus.ACTIVE, now)) {
            throw new ActiveReservationExistsException(book.getId());
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setReservedAt(now);
        reservation.setExpiresAt(now.plusDays(RESERVATION_DURATION_DAYS));
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);

        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        return mapToResponse(savedReservation);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getStatus().name(),
                reservation.getReservedAt(),
                reservation.getExpiresAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
