package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.FollowUpFlowStatus;
import co.sena.edu.themis.Repository.FollowUpFlowStatusRepository;
import co.sena.edu.themis.Service.Dao.Idao;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FollowUpFlowStatusService implements Idao<FollowUpFlowStatus, Long> {

    @Autowired
    private FollowUpFlowStatusRepository followUpFlowStatusRepository;

    public final ModelMapper modelMapper = new ModelMapper();

    @Override
    public List<FollowUpFlowStatus> findAll() {
        return followUpFlowStatusRepository.findAll();
    }

    @Override
    public FollowUpFlowStatus getById(Long id) {
        return followUpFlowStatusRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    public FollowUpFlowStatus save(FollowUpFlowStatus followUpFlowStatus) {
        return followUpFlowStatusRepository.save(followUpFlowStatus);
    }

    @Override
    public void deleteById(Long id) {
        followUpFlowStatusRepository.deleteById(id);
    }

    @Override
    public Page<FollowUpFlowStatus> findAll(Pageable pageable) {
        return followUpFlowStatusRepository.findAll(pageable);
    }

    public boolean existsId(Long id) {
        return followUpFlowStatusRepository.existsById(id);
    }

    // Métodos específicos para FollowUpFlowStatus
    public FollowUpFlowStatus findByName(String name) {
        return followUpFlowStatusRepository.findByName(name);
    }
}
