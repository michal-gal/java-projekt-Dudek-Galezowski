package projekt.pk.edu.pl.bookstore.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import projekt.pk.edu.pl.bookstore.entity.BorrowHistory;

import java.util.Optional;

public interface BorrowHistoryRepository extends JpaRepository<BorrowHistory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT bh
            FROM BorrowHistory bh
            JOIN FETCH bh.user
            JOIN FETCH bh.book
            LEFT JOIN FETCH bh.reservation
            WHERE bh.id = :id
            """)
    Optional<BorrowHistory> findByIdForUpdate(@Param("id") Long id);
}