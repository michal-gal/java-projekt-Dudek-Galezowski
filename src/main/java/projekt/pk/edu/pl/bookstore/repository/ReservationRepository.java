package projekt.pk.edu.pl.bookstore.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import projekt.pk.edu.pl.bookstore.entity.Reservation;
import projekt.pk.edu.pl.bookstore.entity.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            SELECT r FROM Reservation r JOIN FETCH r.book JOIN FETCH r.user
            WHERE r.user.email = :email
            ORDER BY r.reservedAt DESC
            """)
    List<Reservation> findAllByUserEmail(@Param("email") String email);

    @Query("""
            SELECT r FROM Reservation r JOIN FETCH r.book JOIN FETCH r.user
            WHERE r.status = :status
            ORDER BY r.reservedAt ASC
            """)
    List<Reservation> findAllByStatus(@Param("status") ReservationStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Reservation r
            JOIN FETCH r.user
            JOIN FETCH r.book
            WHERE r.id = :id
            """)
    java.util.Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT COUNT(r) > 0
            FROM Reservation r
            WHERE r.user.id = :userId
              AND r.book.id = :bookId
              AND r.status = :status
              AND r.expiresAt > :referenceTime
            """)
    boolean existsActiveReservation(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("status") ReservationStatus status,
            @Param("referenceTime") LocalDateTime referenceTime
    );
}