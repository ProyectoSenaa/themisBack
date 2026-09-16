package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.FollowUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowUpStatusRepository extends JpaRepository<FollowUpStatus, Long> {
    
    Optional<FollowUpStatus> findByName(String name);

    boolean existsByName(String name);
}
