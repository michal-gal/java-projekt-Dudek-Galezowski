package projekt.pk.edu.pl.bookstore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import projekt.pk.edu.pl.bookstore.dto.borrow.BorrowRequest;
import projekt.pk.edu.pl.bookstore.dto.borrow.BorrowResponse;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.entity.BorrowHistory;
import projekt.pk.edu.pl.bookstore.entity.BorrowStatus;
import projekt.pk.edu.pl.bookstore.entity.Reservation;
import projekt.pk.edu.pl.bookstore.entity.ReservationStatus;
import projekt.pk.edu.pl.bookstore.entity.Role;
import projekt.pk.edu.pl.bookstore.entity.User;
import projekt.pk.edu.pl.bookstore.exception.BookUnavailableException;
import projekt.pk.edu.pl.bookstore.exception.BusinessConflictException;
import projekt.pk.edu.pl.bookstore.repository.BookRepository;
import projekt.pk.edu.pl.bookstore.repository.BorrowHistoryRepository;
import projekt.pk.edu.pl.bookstore.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private BorrowHistoryRepository borrowHistoryRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BorrowService borrowService;

    @Test
    void shouldBorrowBookFromActiveReservation() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, "Effective Java", 2, 3);
        Reservation reservation = reservation(8L, user, book, ReservationStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        when(reservationRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(reservation));
        when(bookRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(book));
        when(borrowHistoryRepository.saveAndFlush(any(BorrowHistory.class)))
                .thenAnswer(invocation -> {
                    BorrowHistory history = invocation.getArgument(0);
                    history.setId(12L);
                    return history;
                });

        BorrowResponse response = borrowService.borrowBook(new BorrowRequest(8L), " READER@example.com ");

        ArgumentCaptor<BorrowHistory> captor = ArgumentCaptor.forClass(BorrowHistory.class);
        verify(borrowHistoryRepository).saveAndFlush(captor.capture());
        BorrowHistory savedHistory = captor.getValue();

        assertThat(book.getAvailableCopies()).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
        assertThat(reservation.getFulfilledAt()).isNotNull();
        assertThat(savedHistory.getStatus()).isEqualTo(BorrowStatus.BORROWED);
        assertThat(savedHistory.getDueAt()).isEqualTo(savedHistory.getBorrowedAt().plusDays(14));
        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.bookId()).isEqualTo(9L);
        assertThat(response.bookTitle()).isEqualTo("Effective Java");
        assertThat(response.status()).isEqualTo("BORROWED");
        assertThat(response.reservationId()).isEqualTo(8L);
    }

    @Test
    void shouldRejectBorrowingAnotherUsersReservation() {
        User owner = user(4L, "owner@example.com");
        Book book = book(9L, "Effective Java", 2, 3);
        Reservation reservation = reservation(8L, owner, book, ReservationStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        when(reservationRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> borrowService.borrowBook(new BorrowRequest(8L), "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Rezerwacja nie należy do bieżącego użytkownika.");
    }

    @Test
    void shouldRejectExpiredReservation() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, "Effective Java", 2, 3);
        Reservation reservation = reservation(8L, user, book, ReservationStatus.ACTIVE, LocalDateTime.now().minusMinutes(1));

        when(reservationRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> borrowService.borrowBook(new BorrowRequest(8L), "reader@example.com"))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("Nie można zrealizować wygasłej rezerwacji.");
    }

    @Test
    void shouldThrowWhenBorrowingUnavailableBook() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, "Effective Java", 0, 3);
        Reservation reservation = reservation(8L, user, book, ReservationStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        when(reservationRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(reservation));
        when(bookRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> borrowService.borrowBook(new BorrowRequest(8L), "reader@example.com"))
                .isInstanceOf(BookUnavailableException.class)
                .hasMessage("Brak dostępnych egzemplarzy książki o ID: 9");
    }

    @Test
    void shouldReturnBorrowedBook() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, "Effective Java", 1, 3);
        BorrowHistory borrowHistory = borrowHistory(12L, user, book, BorrowStatus.BORROWED);

        when(borrowHistoryRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(borrowHistory));
        when(bookRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(book));
        when(borrowHistoryRepository.saveAndFlush(any(BorrowHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BorrowResponse response = borrowService.returnBook(12L, "READER@example.com");

        assertThat(book.getAvailableCopies()).isEqualTo(2);
        assertThat(borrowHistory.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(borrowHistory.getReturnedAt()).isNotNull();
        assertThat(response.status()).isEqualTo("RETURNED");
        assertThat(response.returnedAt()).isNotNull();
    }

    @Test
    void shouldRejectReturningAlreadyReturnedBorrow() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, "Effective Java", 1, 3);
        BorrowHistory borrowHistory = borrowHistory(12L, user, book, BorrowStatus.RETURNED);

        when(borrowHistoryRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(borrowHistory));

        assertThatThrownBy(() -> borrowService.returnBook(12L, "reader@example.com"))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("Nie można zwrócić wypożyczenia o statusie: RETURNED");
    }

    @Test
    void shouldReturnCurrentUserBorrowHistoryWithNormalizedEmail() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, "Effective Java", 1, 3);
        Reservation reservation = reservation(8L, user, book, ReservationStatus.FULFILLED, LocalDateTime.now().plusDays(1));
        BorrowHistory borrowHistory = borrowHistory(12L, user, book, BorrowStatus.BORROWED);
        borrowHistory.setReservation(reservation);

        when(borrowHistoryRepository.findAllByUserEmailOrderByBorrowedAtDesc("reader@example.com"))
                .thenReturn(List.of(borrowHistory));

        List<BorrowResponse> history = borrowService.getCurrentUserHistory(" READER@example.com ");

        verify(borrowHistoryRepository).findAllByUserEmailOrderByBorrowedAtDesc("reader@example.com");
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().reservationId()).isEqualTo(8L);
        assertThat(history.getFirst().status()).isEqualTo("BORROWED");
    }

    private User user(Long id, String email) {
        Role role = new Role();
        role.setName("READER");
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Book book(Long id, String title, int availableCopies, int totalCopies) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAvailableCopies(availableCopies);
        book.setTotalCopies(totalCopies);
        return book;
    }

    private Reservation reservation(Long id, User user, Book book, ReservationStatus status, LocalDateTime expiresAt) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(status);
        reservation.setExpiresAt(expiresAt);
        return reservation;
    }

    private BorrowHistory borrowHistory(Long id, User user, Book book, BorrowStatus status) {
        BorrowHistory borrowHistory = new BorrowHistory();
        borrowHistory.setId(id);
        borrowHistory.setUser(user);
        borrowHistory.setBook(book);
        borrowHistory.setStatus(status);
        borrowHistory.setBorrowedAt(LocalDateTime.now().minusDays(1));
        borrowHistory.setDueAt(LocalDateTime.now().plusDays(13));
        return borrowHistory;
    }
}
