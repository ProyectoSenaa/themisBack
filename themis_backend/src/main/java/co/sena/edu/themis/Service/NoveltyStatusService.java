package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.NoveltyStatus;
import co.sena.edu.themis.Repository.NoveltyStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NoveltyStatusService {

    @Autowired
    private NoveltyStatusRepository noveltyStatusRepository;

    public NoveltyStatus findByName(String name) {
        Optional<NoveltyStatus> status = noveltyStatusRepository.findByName(name);
        return status.orElseGet(() -> {
            // Si no existe, crear uno nuevo
            NoveltyStatus newStatus = new NoveltyStatus();
            newStatus.setName(name);
            newStatus.setDescription("Estado de novedad: " + name);
            return noveltyStatusRepository.save(newStatus);
        });
    }

    public NoveltyStatus save(NoveltyStatus noveltyStatus) {
        return noveltyStatusRepository.save(noveltyStatus);
    }

    public NoveltyStatus getById(Long id) {
        return noveltyStatusRepository.findById(id).orElse(null);
    }
}
