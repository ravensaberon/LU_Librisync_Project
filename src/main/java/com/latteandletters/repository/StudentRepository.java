package com.latteandletters.repository;

import com.latteandletters.model.Student;
import com.latteandletters.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByUser_Id(Long userId);

    Optional<Student> findTopByOrderByIdDesc();

    List<Student> findAllByOrderByStudentIdAsc();

    List<Student> findByStudentIdContainingIgnoreCaseOrderByStudentIdAsc(String studentId);

    long countByUser_Status(UserStatus status);

    boolean existsByPhone(String phone);
}
