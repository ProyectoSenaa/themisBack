package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.FollowUpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowUpTypeRepository extends JpaRepository<FollowUpType, Long> {
    
    @Query("SELECT ft.name FROM FollowUpType ft WHERE ft.isActive = true")
    List<String> findAllActiveFollowUpTypesOnly();
    
    @Query("SELECT ft FROM FollowUpType ft WHERE ft.isActive = true ORDER BY ft.name ASC")
    List<FollowUpType> findAllActiveOrderByName();

    List<FollowUpType> findByIsActiveTrue();

    FollowUpType findByName(String name);

    boolean existsByName(String name);
}
