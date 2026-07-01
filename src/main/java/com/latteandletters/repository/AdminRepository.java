package com.latteandletters.repository;

import com.latteandletters.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUser_EmailIgnoreCase(String email);
}
