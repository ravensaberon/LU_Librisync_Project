package com.lulibrisync.dto;

import com.lulibrisync.model.Student;

public record StudentRegistrationResult(Student student,
                                        String temporaryPassword) {
}
