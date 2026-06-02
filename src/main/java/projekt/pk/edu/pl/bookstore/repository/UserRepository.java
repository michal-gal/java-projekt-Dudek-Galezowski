package projekt.pk.edu.pl.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projekt.pk.edu.pl.bookstore.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}