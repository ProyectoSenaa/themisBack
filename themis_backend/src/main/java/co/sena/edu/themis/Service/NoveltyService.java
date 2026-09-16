package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Repository.NoveltyRepository;

import co.sena.edu.themis.Service.Dao.Idao;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NoveltyService implements Idao<Novelty, Long> {
    @Autowired
    private NoveltyRepository noveltyRepository;
    @Override
    public List<Novelty> findAll() {
        return noveltyRepository.findAll();
    }

    public final ModelMapper modelMapper = new ModelMapper();

    @Override
    public Novelty getById(Long id) {
        return noveltyRepository.findById(id).orElseThrow()     ;
    }

    @Override
    public Novelty save(Novelty novelty) {
        return noveltyRepository.save(novelty);
    }

    public boolean existsId(Long id) {
        return noveltyRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        noveltyRepository.deleteById(id);
    }

    @Override
    public Page<Novelty> findAll(Pageable pageable) {
        return this.noveltyRepository.findAll(pageable);
    }

    public List<String> getAllNoveltyTypesRaw() {
        return noveltyRepository.findAllNoveltyTypesOnly();
    }
    @Transactional

    public List<Novelty> getAllWithType() {
        return noveltyRepository.findAllWithTypeForced();
    }

    public List<Novelty> findAllByIdStudent(Long studentId) {
        return noveltyRepository.findAllByIdStudent(studentId);
    }

    public List<Novelty> findAllByIdTeacher(Long teacherId) {
        return noveltyRepository.findAllByIdTeacher(teacherId);
    }

    public List<Novelty> findAllByIdAdministrative(Long administrativeId) {
        return noveltyRepository.findAllByIdAdministrative(administrativeId);
    }


    public boolean existsBlockingForStudent(Long studentId, List<String> blockingStatuses) {
        List<String> lower = blockingStatuses.stream()
                .filter(s -> s != null)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        return noveltyRepository.existsBlockingForStudent(studentId, lower);
    }

    public List<Novelty> findAllByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        return noveltyRepository.findByStudentIdIn(studentIds);
    }
}
