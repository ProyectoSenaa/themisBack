package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.FollowUpStatus;
import co.sena.edu.themis.Repository.FollowUpStatusRepository;
import co.sena.edu.themis.Service.Dao.Idao;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FollowUpStatusService implements Idao<FollowUpStatus, Long> {

    @Autowired
    private FollowUpStatusRepository followUpStatusRepository;

    public final ModelMapper modelMapper = new ModelMapper();

    @Override
    public List<FollowUpStatus> findAll() {
        return followUpStatusRepository.findAll();
    }

    @Override
    public FollowUpStatus getById(Long id) {
        return followUpStatusRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    public FollowUpStatus save(FollowUpStatus followUpStatus) {
        return followUpStatusRepository.save(followUpStatus);
    }

    @Override
    public void deleteById(Long id) {
        followUpStatusRepository.deleteById(id);
    }

    @Override
    public Page<FollowUpStatus> findAll(Pageable pageable) {
        return followUpStatusRepository.findAll(pageable);
    }

    public boolean existsId(Long id) {
        return followUpStatusRepository.existsById(id);
    }

    // Métodos específicos para FollowUpStatus
    public Optional<FollowUpStatus> findByName(String name) {
        return followUpStatusRepository.findByName(name);
    }

    // Agrega este método para buscar por nombre
    public FollowUpStatus getByName(String name) {
        return followUpStatusRepository.findByName(name).orElseThrow();
    }
}
