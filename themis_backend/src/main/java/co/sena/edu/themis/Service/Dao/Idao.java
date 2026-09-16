package co.sena.edu.themis.Service.Dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface Idao <T, ID>{
    List<T> findAll();
    T getById(ID id);
    T save(T entity);
    void deleteById(ID id);
    Page<T> findAll(Pageable pageable);


}