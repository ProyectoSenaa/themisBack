package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.FollowUpFlowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowUpFlowStatusRepository extends JpaRepository<FollowUpFlowStatus, Long> {
    
    FollowUpFlowStatus findByName(String name);

    boolean existsByName(String name);
}
