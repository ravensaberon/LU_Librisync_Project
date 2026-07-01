package com.latteandletters.repository;

import com.latteandletters.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    Optional<Category> findByNameIgnoreCase(String name);

    Optional<Category> findByNameIgnoreCaseAndIdNot(String name, Long id);
}
