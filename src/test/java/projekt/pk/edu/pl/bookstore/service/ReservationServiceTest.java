package projekt.pk.edu.pl.bookstore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import projekt.pk.edu.pl.bookstore.dto.reservation.CreateReservationRequest;
import projekt.pk.edu.pl.bookstore.dto.reservation.ReservationResponse;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.entity.Reservation;
import projekt.pk.edu.pl.bookstore.entity.ReservationStatus;
import projekt.pk.edu.pl.bookstore.entity.Role;
import projekt.pk.edu.pl.bookstore.entity.User;
import projekt.pk.edu.pl.bookstore.exception.ActiveReservationExistsException;
import projekt.pk.edu.pl.bookstore.exception.BookUnavailableException;
import projekt.pk.edu.pl.bookstore.exception.ResourceNotFoundException;
import projekt.pk.edu.pl.bookstore.repository.BookRepository;
import projekt.pk.edu.pl.bookstore.repository.ReservationRepository;
import projekt.pk.edu.pl.bookstore.repository.UserRepository;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void shouldCreateActiveReservation() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, 2, 5);

        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(bookRepository.findById(9L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsActiveReservation(eq(4L), eq(9L), eq(ReservationStatus.ACTIVE), any()))
                .thenReturn(false);
        when(reservationRepository.saveAndFlush(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    reservation.setId(15L);
                    return reservation;
                });

        ReservationResponse response = reservationService.createReservation(new CreateReservationRequest(9L), " READER@example.com ");

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).saveAndFlush(captor.capture());
        Reservation savedReservation = captor.getValue();

        assertThat(savedReservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(savedReservation.getReservedAt()).isNotNull();
        assertThat(savedReservation.getExpiresAt()).isEqualTo(savedReservation.getReservedAt().plusDays(3));
        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.userId()).isEqualTo(4L);
        assertThat(response.bookId()).isEqualTo(9L);
        assertThat(response.bookTitle()).isEqualTo(book.getTitle());
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(ChronoUnit.DAYS.between(response.reservedAt(), response.expiresAt())).isEqualTo(3);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(new CreateReservationRequest(1L), "missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Nie znaleziono użytkownika o emailu: missing@example.com");
    }

    @Test
    void shouldThrowWhenBookIsUnavailable() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, 0, 5);

        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(bookRepository.findById(9L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> reservationService.createReservation(new CreateReservationRequest(9L), "reader@example.com"))
                .isInstanceOf(BookUnavailableException.class)
                .hasMessage("Brak dostępnych egzemplarzy książki o ID: 9");
    }

    @Test
    void shouldThrowWhenActiveReservationAlreadyExists() {
        User user = user(4L, "reader@example.com");
        Book book = book(9L, 2, 5);

        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(bookRepository.findById(9L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsActiveReservation(eq(4L), eq(9L), eq(ReservationStatus.ACTIVE), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(new CreateReservationRequest(9L), "reader@example.com"))
                .isInstanceOf(ActiveReservationExistsException.class)
                .hasMessage("Użytkownik posiada już aktywną rezerwację dla książki o ID: 9");
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

    private Book book(Long id, int availableCopies, int totalCopies) {
        Book book = new Book();
        book.setId(id);
        book.setTitle("Clean Code");
        book.setAvailableCopies(availableCopies);
        book.setTotalCopies(totalCopies);
        return book;
    }
}
