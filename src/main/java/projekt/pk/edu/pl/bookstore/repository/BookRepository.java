package projekt.pk.edu.pl.bookstore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import projekt.pk.edu.pl.bookstore.entity.Book;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT b FROM Book b WHERE " +
            "(COALESCE(:query, '') = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%')) OR " +
            "LOWER(b.isbn) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%')))")
    Page<Book> searchBooks(@Param("query") String query, Pageable pageable);
}