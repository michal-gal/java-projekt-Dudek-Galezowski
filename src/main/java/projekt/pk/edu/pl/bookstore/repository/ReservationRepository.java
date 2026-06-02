package projekt.pk.edu.pl.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projekt.pk.edu.pl.bookstore.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}