package projekt.pk.edu.pl.bookstore.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projekt.pk.edu.pl.bookstore.dto.borrow.BorrowRequest;
import projekt.pk.edu.pl.bookstore.dto.borrow.BorrowResponse;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.entity.BorrowHistory;
import projekt.pk.edu.pl.bookstore.entity.BorrowStatus;
import projekt.pk.edu.pl.bookstore.entity.Reservation;
import projekt.pk.edu.pl.bookstore.entity.ReservationStatus;
import projekt.pk.edu.pl.bookstore.exception.BusinessConflictException;
import projekt.pk.edu.pl.bookstore.exception.BookUnavailableException;
import projekt.pk.edu.pl.bookstore.exception.ResourceNotFoundException;
import projekt.pk.edu.pl.bookstore.repository.BookRepository;
import projekt.pk.edu.pl.bookstore.repository.BorrowHistoryRepository;
import projekt.pk.edu.pl.bookstore.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BorrowService {

    private static final long BORROW_DURATION_DAYS = 14;

    private final BorrowHistoryRepository borrowHistoryRepository;
    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;

    public BorrowService(
            BorrowHistoryRepository borrowHistoryRepository,
            ReservationRepository reservationRepository,
            BookRepository bookRepository
    ) {
        this.borrowHistoryRepository = borrowHistoryRepository;
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public BorrowResponse borrowBook(BorrowRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Reservation reservation = reservationRepository.findByIdForUpdate(request.reservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono rezerwacji o ID: " + request.reservationId()));

        ensureReservationCanBeBorrowed(reservation, now);

        Book book = bookRepository.findByIdForUpdate(reservation.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono książki o ID: " + reservation.getBook().getId()));
        if (book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException(book.getId());
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        book.setUpdatedAt(now);

        reservation.setStatus(ReservationStatus.FULFILLED);
        reservation.setFulfilledAt(now);
        reservation.setUpdatedAt(now);

        BorrowHistory borrowHistory = new BorrowHistory();
        borrowHistory.setUser(reservation.getUser());
        borrowHistory.setBook(book);
        borrowHistory.setReservation(reservation);
        borrowHistory.setStatus(BorrowStatus.BORROWED);
        borrowHistory.setBorrowedAt(now);
        borrowHistory.setDueAt(now.plusDays(BORROW_DURATION_DAYS));
        borrowHistory.setCreatedAt(now);
        borrowHistory.setUpdatedAt(now);

        BorrowHistory savedBorrowHistory = borrowHistoryRepository.saveAndFlush(borrowHistory);
        return mapToResponse(savedBorrowHistory);
    }

    @Transactional
    public BorrowResponse returnBook(Long borrowId, String email) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();

        BorrowHistory borrowHistory = borrowHistoryRepository.findByIdForUpdate(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono wypożyczenia o ID: " + borrowId));

        ensureBorrowOwner(borrowHistory, normalizedEmail);
        if (borrowHistory.getStatus() != BorrowStatus.BORROWED && borrowHistory.getStatus() != BorrowStatus.OVERDUE) {
            throw new BusinessConflictException("Nie można zwrócić wypożyczenia o statusie: " + borrowHistory.getStatus());
        }

        Book book = bookRepository.findByIdForUpdate(borrowHistory.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono książki o ID: " + borrowHistory.getBook().getId()));
        if (book.getAvailableCopies() >= book.getTotalCopies()) {
            throw new BusinessConflictException("Nie można zwiększyć liczby dostępnych egzemplarzy ponad stan magazynowy.");
        }

        book.setAvailableCopies(book.getAvailableCopies() + 1);
        book.setUpdatedAt(now);

        borrowHistory.setStatus(BorrowStatus.RETURNED);
        borrowHistory.setReturnedAt(now);
        borrowHistory.setUpdatedAt(now);

        BorrowHistory savedBorrowHistory = borrowHistoryRepository.saveAndFlush(borrowHistory);
        return mapToResponse(savedBorrowHistory);
    }

    @Transactional(readOnly = true)
    public List<BorrowResponse> getCurrentUserHistory(String email) {
        String normalizedEmail = normalizeEmail(email);
        return borrowHistoryRepository.findAllByUserEmailOrderByBorrowedAtDesc(normalizedEmail)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowResponse> getAllActiveBorrows() {
        return borrowHistoryRepository.findAllByStatus(BorrowStatus.BORROWED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void ensureReservationCanBeBorrowed(Reservation reservation, LocalDateTime now) {
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessConflictException("Nie można zrealizować rezerwacji o statusie: " + reservation.getStatus());
        }

        if (!reservation.getExpiresAt().isAfter(now)) {
            throw new BusinessConflictException("Nie można zrealizować wygasłej rezerwacji.");
        }
    }

    private void ensureBorrowOwner(BorrowHistory borrowHistory, String normalizedEmail) {
        if (!borrowHistory.getUser().getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new AccessDeniedException("Wypożyczenie nie należy do bieżącego użytkownika.");
        }
    }

    private BorrowResponse mapToResponse(BorrowHistory borrowHistory) {
        return new BorrowResponse(
                borrowHistory.getId(),
                borrowHistory.getUser().getId(),
                borrowHistory.getUser().getEmail(),
                borrowHistory.getBook().getId(),
                borrowHistory.getBook().getTitle(),
                borrowHistory.getReservation() != null ? borrowHistory.getReservation().getId() : null,
                borrowHistory.getStatus().name(),
                borrowHistory.getBorrowedAt(),
                borrowHistory.getDueAt(),
                borrowHistory.getReturnedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
