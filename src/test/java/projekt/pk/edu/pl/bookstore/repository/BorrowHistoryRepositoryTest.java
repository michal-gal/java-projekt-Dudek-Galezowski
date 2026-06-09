package projekt.pk.edu.pl.bookstore.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.entity.BorrowHistory;
import projekt.pk.edu.pl.bookstore.entity.BorrowStatus;
import projekt.pk.edu.pl.bookstore.entity.Reservation;
import projekt.pk.edu.pl.bookstore.entity.ReservationStatus;
import projekt.pk.edu.pl.bookstore.entity.Role;
import projekt.pk.edu.pl.bookstore.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BorrowHistoryRepositoryTest {

    @Autowired
    private BorrowHistoryRepository borrowHistoryRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReturnBorrowHistoryForUserOrderedByBorrowedAtDescending() {
        User user = createUser("Reader");
        User otherUser = createUser("Other");
        Book olderBook = createBook("Older Book");
        Book newerBook = createBook("Newer Book");
        Reservation reservation = createReservation(user, newerBook);
        BorrowHistory olderBorrow = createBorrowHistory(
                user,
                olderBook,
                null,
                BorrowStatus.RETURNED,
                LocalDateTime.now().minusDays(5)
        );
        BorrowHistory newerBorrow = createBorrowHistory(
                user,
                newerBook,
                reservation,
                BorrowStatus.BORROWED,
                LocalDateTime.now().minusDays(1)
        );
        createBorrowHistory(
                otherUser,
                createBook("Ignored Book"),
                null,
                BorrowStatus.BORROWED,
                LocalDateTime.now().minusHours(2)
        );

        List<BorrowHistory> result = borrowHistoryRepository
                .findAllByUserEmailOrderByBorrowedAtDesc(user.getEmail().toUpperCase());

        assertThat(result).extracting(BorrowHistory::getId).containsExactly(newerBorrow.getId(), olderBorrow.getId());
        assertThat(result).extracting(BorrowHistory::getStatus).containsExactly(BorrowStatus.BORROWED, BorrowStatus.RETURNED);
        assertThat(result.getFirst().getReservation()).isNotNull();
    }

    @Test
    void shouldFindBorrowHistoryForUpdateWithLoadedAssociations() {
        User user = createUser("Reader");
        Book book = createBook("Clean Architecture");
        Reservation reservation = createReservation(user, book);
        BorrowHistory borrowHistory = createBorrowHistory(
                user,
                book,
                reservation,
                BorrowStatus.BORROWED,
                LocalDateTime.now().minusDays(2)
        );

        BorrowHistory foundBorrowHistory = borrowHistoryRepository.findByIdForUpdate(borrowHistory.getId()).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(foundBorrowHistory.getId()).isEqualTo(borrowHistory.getId());
        assertThat(persistenceUnitUtil.isLoaded(foundBorrowHistory.getUser())).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(foundBorrowHistory.getBook())).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(foundBorrowHistory.getReservation())).isTrue();
        assertThat(foundBorrowHistory.getUser().getEmail()).isEqualTo(user.getEmail());
        assertThat(foundBorrowHistory.getBook().getTitle()).isEqualTo(book.getTitle());
        assertThat(foundBorrowHistory.getReservation().getId()).isEqualTo(reservation.getId());
    }

    private User createUser(String prefix) {
        Role readerRole = roleRepository.findByName("READER").orElseThrow();

        User user = new User();
        user.setRole(readerRole);
        user.setFirstName(prefix);
        user.setLastName("Test");
        user.setEmail(prefix.toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("hashed-password");
        user.setIsActive(true);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Book createBook(String title) {
        Book book = new Book();
        LocalDateTime now = LocalDateTime.now();
        book.setIsbn(UUID.randomUUID().toString().replace("-", "").substring(0, 13));
        book.setTitle(title);
        book.setAuthor("Test Author");
        book.setPublisher("Test Publisher");
        book.setPublicationYear(2024);
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        entityManager.persist(book);
        entityManager.flush();
        return book;
    }

    private Reservation createReservation(User user, Book book) {
        Reservation reservation = new Reservation();
        LocalDateTime now = LocalDateTime.now();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setReservedAt(now.minusDays(3));
        reservation.setExpiresAt(now.plusDays(2));
        reservation.setCreatedAt(now.minusDays(3));
        reservation.setUpdatedAt(now.minusDays(3));
        entityManager.persist(reservation);
        entityManager.flush();
        return reservation;
    }

    private BorrowHistory createBorrowHistory(
            User user,
            Book book,
            Reservation reservation,
            BorrowStatus status,
            LocalDateTime borrowedAt
    ) {
        BorrowHistory borrowHistory = new BorrowHistory();
        LocalDateTime updatedAt = borrowedAt.plusHours(1);
        borrowHistory.setUser(user);
        borrowHistory.setBook(book);
        borrowHistory.setReservation(reservation);
        borrowHistory.setStatus(status);
        borrowHistory.setBorrowedAt(borrowedAt);
        borrowHistory.setDueAt(borrowedAt.plusDays(14));
        borrowHistory.setReturnedAt(status == BorrowStatus.RETURNED ? borrowedAt.plusDays(3) : null);
        borrowHistory.setCreatedAt(borrowedAt);
        borrowHistory.setUpdatedAt(updatedAt);
        entityManager.persist(borrowHistory);
        entityManager.flush();
        return borrowHistory;
    }
}
