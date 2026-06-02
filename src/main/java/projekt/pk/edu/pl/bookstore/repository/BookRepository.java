package projekt.pk.edu.pl.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projekt.pk.edu.pl.bookstore.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
    Book findByIsbn(String isbn);
}