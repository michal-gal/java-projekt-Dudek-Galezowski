package projekt.pk.edu.pl.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projekt.pk.edu.pl.bookstore.entity.BorrowHistory;

public interface BorrowHistoryRepository extends JpaRepository<BorrowHistory, Long> {
}