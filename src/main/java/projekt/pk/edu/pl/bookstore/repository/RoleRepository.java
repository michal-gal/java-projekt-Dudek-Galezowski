package projekt.pk.edu.pl.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projekt.pk.edu.pl.bookstore.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}