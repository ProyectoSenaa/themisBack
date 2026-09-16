package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.Committee;
import co.sena.edu.themis.Repository.CommitteeRepository;
import co.sena.edu.themis.Service.Dao.Idao;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommitteeService implements Idao<Committee, Long> {

    private final CommitteeRepository committeeRepository;
    private final ModelMapper modelMapper = new ModelMapper();

    @Autowired
    public CommitteeService(CommitteeRepository committeeRepository) {
        this.committeeRepository = committeeRepository;
    }

    @Override
    public List<Committee> findAll() {
        return committeeRepository.findAll();
    }

    @Override
    public Committee getById(Long id) {
        return committeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Committee not found with id: " + id));
    }

    @Override
    public Committee save(Committee committee) {
        return committeeRepository.save(committee);
    }

    public boolean existsId(Long id) {
        return committeeRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        committeeRepository.deleteById(id);
    }

    @Override
    public Page<Committee> findAll(Pageable pageable) {
        return committeeRepository.findAll(pageable);
    }

    @Transactional
    public List<Committee> getAllActive() {
        return committeeRepository.findAllByIsActiveTrue();
    }

    public List<Committee> findCommitteesByStudentId(Long studentId) {
        return committeeRepository.findCommitteesByStudentsIdsContaining(studentId);
    }

    public List<Committee> findCommitteesByTeacherId(Long teacherId) {
        return committeeRepository.findCommitteesByTeachersIdsContaining(teacherId);
    }

    public List<Committee> findCommitteesByAdministrativeId(Long administrativeId) {
        return committeeRepository.findCommitteesByAdministrativesIdsContaining(administrativeId);
    }

    public List<Committee> findActiveCommittees() {
        return committeeRepository.findAllByIsActiveTrue();
    }

    public List<Committee> findCommitteesByCoordinationId(Long coordinationId) {
        return committeeRepository.findAllByCoordinationIdAndIsActiveTrue(coordinationId);
    }

}