package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.FollowUp;
import co.sena.edu.themis.Repository.FollowUpRepository;
import co.sena.edu.themis.Service.Dao.Idao;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FollowUpService implements Idao<FollowUp, Long> {

    @Autowired
    private FollowUpRepository followUpRepository;

    public final ModelMapper modelMapper = new ModelMapper();

    @Override
    public List<FollowUp> findAll() {
        return followUpRepository.findAll();
    }

    @Override
    public FollowUp getById(Long id) {
        return followUpRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    public FollowUp save(FollowUp followUp) {
        return followUpRepository.saveAndFlush(followUp);
    }

    @Override
    public void deleteById(Long id) {
        followUpRepository.deleteById(id);
    }

    @Override
    public Page<FollowUp> findAll(Pageable pageable) {
        return followUpRepository.findAllWithRelations(pageable);
    }

    public boolean existsId(Long id) {
        return followUpRepository.existsById(id);
    }

    // Métodos específicos para el Módulo de Gestión de Casos
    public List<FollowUp> findAllActiveCases() {
        return followUpRepository.findAllActiveCases();
    }

    public List<FollowUp> findCasesByStudentId(Long studentId) {
        return followUpRepository.findByStudentIdAndActiveTrue(studentId);
    }

    public List<FollowUp> findCasesByTeacherId(Long teacherId) {
        return followUpRepository.findByTeacherIdAndActiveTrue(teacherId);
    }

    @Transactional
    public FollowUp createCase(FollowUp followUp) {
        followUp.setCreationDate(LocalDate.now());
        followUp.setIsActive(true);
        return followUpRepository.save(followUp);
    }

    // Métodos de consulta por coordinador
    public List<FollowUp> findByCoordinatorId(Long coordinatorId) {
        return followUpRepository.findByCoordinatorId(coordinatorId);
    }

    // Métodos de consulta por evento de comité
    public List<FollowUp> findByCommitteeEventId(Long committeeEventId) {
        return followUpRepository.findByCommitteeEventId(committeeEventId);
    }

    // Métodos con relación a Minute
    public List<FollowUp> findCasesWithMinutes() {
        return followUpRepository.findCasesWithMinutes();
    }

    // Métodos de estadísticas y reportes
    public Long countByTypeAndDateRange(Long typeId, LocalDate startDate, LocalDate endDate) {
        return followUpRepository.countByTypeAndDateRange(typeId, startDate, endDate);
    }

    public List<Object[]> getFollowUpTypeStatistics() {
        return followUpRepository.findFollowUpTypeStatistics();
    }
}