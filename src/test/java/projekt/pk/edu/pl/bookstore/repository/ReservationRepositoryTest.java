package projekt.pk.edu.pl.bookstore.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.entity.Reservation;
import projekt.pk.edu.pl.bookstore.entity.ReservationStatus;
import projekt.pk.edu.pl.bookstore.entity.Role;
import projekt.pk.edu.pl.bookstore.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldDetectOnlyUnexpiredActiveReservationForUserAndBook() {
        User user = createUser();
        Book book = createBook();
        createReservation(user, book, ReservationStatus.ACTIVE, LocalDateTime.now().plusDays(2));
        createReservation(user, book, ReservationStatus.EXPIRED, LocalDateTime.now().plusDays(2));
        createReservation(user, book, ReservationStatus.ACTIVE, LocalDateTime.now().minusMinutes(5));

        boolean exists = reservationRepository.existsActiveReservation(
                user.getId(),
                book.getId(),
                ReservationStatus.ACTIVE,
                LocalDateTime.now()
        );

        boolean existsForOtherStatus = reservationRepository.existsActiveReservation(
                user.getId(),
                book.getId(),
                ReservationStatus.FULFILLED,
                LocalDateTime.now()
        );

        assertThat(exists).isTrue();
        assertThat(existsForOtherStatus).isFalse();
    }

    @Test
    void shouldFindReservationForUpdateWithLoadedUserAndBook() {
        User user = createUser();
        Book book = createBook();
        Reservation reservation = createReservation(
                user,
                book,
                ReservationStatus.ACTIVE,
                LocalDateTime.now().plusDays(3)
        );

        Reservation foundReservation = reservationRepository.findByIdForUpdate(reservation.getId()).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(foundReservation.getId()).isEqualTo(reservation.getId());
        assertThat(persistenceUnitUtil.isLoaded(foundReservation.getUser())).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(foundReservation.getBook())).isTrue();
        assertThat(foundReservation.getUser().getEmail()).isEqualTo(user.getEmail());
        assertThat(foundReservation.getBook().getTitle()).isEqualTo(book.getTitle());
    }

    private User createUser() {
        Role readerRole = roleRepository.findByName("READER").orElseThrow();

        User user = new User();
        user.setRole(readerRole);
        user.setFirstName("Jan");
        user.setLastName("Kowalski");
        user.setEmail("reader-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("hashed-password");
        user.setIsActive(true);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Book createBook() {
        Book book = new Book();
        LocalDateTime now = LocalDateTime.now();
        book.setIsbn(UUID.randomUUID().toString().replace("-", "").substring(0, 13));
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setPublisher("Test Publisher");
        book.setPublicationYear(2024);
        book.setTotalCopies(4);
        book.setAvailableCopies(4);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        entityManager.persist(book);
        entityManager.flush();
        return book;
    }

    private Reservation createReservation(User user, Book book, ReservationStatus status, LocalDateTime expiresAt) {
        Reservation reservation = new Reservation();
        LocalDateTime now = LocalDateTime.now();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(status);
        reservation.setReservedAt(now.minusHours(1));
        reservation.setExpiresAt(expiresAt);
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        entityManager.persist(reservation);
        entityManager.flush();
        return reservation;
    }
}
