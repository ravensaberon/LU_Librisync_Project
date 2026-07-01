package com.lulibrisync.repository;

import com.lulibrisync.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    List<Author> findAllByOrderByNameAsc();

    Optional<Author> findByNameIgnoreCase(String name);

    Optional<Author> findByNameIgnoreCaseAndIdNot(String name, Long id);
}
