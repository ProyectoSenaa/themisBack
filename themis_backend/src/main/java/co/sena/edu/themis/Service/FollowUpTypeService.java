package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.FollowUpType;
import co.sena.edu.themis.Repository.FollowUpTypeRepository;
import co.sena.edu.themis.Service.Dao.Idao;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FollowUpTypeService implements Idao<FollowUpType, Long> {

    @Autowired
    private FollowUpTypeRepository followUpTypeRepository;

    public final ModelMapper modelMapper = new ModelMapper();

    @Override
    public List<FollowUpType> findAll() {
        return followUpTypeRepository.findAll();
    }

    @Override
    public FollowUpType getById(Long id) {
        return followUpTypeRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    public FollowUpType save(FollowUpType followUpType) {
        return followUpTypeRepository.save(followUpType);
    }

    @Override
    public void deleteById(Long id) {
        followUpTypeRepository.deleteById(id);
    }

    @Override
    public Page<FollowUpType> findAll(Pageable pageable) {
        return followUpTypeRepository.findAll(pageable);
    }

    public boolean existsId(Long id) {
        return followUpTypeRepository.existsById(id);
    }

    // Métodos específicos para FollowUpType
    public List<String> findAllActiveFollowUpTypesOnly() {
        return followUpTypeRepository.findAllActiveFollowUpTypesOnly();
    }

    public List<FollowUpType> findAllActiveOrderByName() {
        return followUpTypeRepository.findAllActiveOrderByName();
    }

    public List<FollowUpType> findActiveTypes() {
        return followUpTypeRepository.findByIsActiveTrue();
    }

    @Transactional
    public FollowUpType activateType(Long id) {
        FollowUpType type = getById(id);
        type.setActive(true);
        return followUpTypeRepository.save(type);
    }

    @Transactional
    public FollowUpType deactivateType(Long id) {
        FollowUpType type = getById(id);
        type.setActive(false);
        return followUpTypeRepository.save(type);
    }
}
