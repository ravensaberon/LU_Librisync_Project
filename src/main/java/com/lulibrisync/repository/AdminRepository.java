package com.lulibrisync.repository;

import com.lulibrisync.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUser_EmailIgnoreCase(String email);
}
