package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.NoveltyType;
import co.sena.edu.themis.Repository.NoveltyTypeRepository;
import co.sena.edu.themis.Service.Dao.Idao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoveltyTypeService implements Idao<NoveltyType, Long> {

    @Autowired
    private NoveltyTypeRepository noveltyTypeRepository;

    @Override
    public List<NoveltyType> findAll() {
        return noveltyTypeRepository.findAll();
    }

    @Override
    public NoveltyType getById(Long id) {
        return noveltyTypeRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("NoveltyType not found with id: " + id));
    }

    @Override
    public NoveltyType save(NoveltyType noveltyType) {
        return noveltyTypeRepository.save(noveltyType);
    }

    public boolean existsNameNovelty(String nameNovelty) {
        return noveltyTypeRepository.existsByNameNovelty(nameNovelty);
    }

    @Override
    public void deleteById(Long id) {
        noveltyTypeRepository.deleteById(id);
    }

    @Override
    public Page<NoveltyType> findAll(Pageable pageable) {
        return noveltyTypeRepository.findAll(pageable);
    }
}
