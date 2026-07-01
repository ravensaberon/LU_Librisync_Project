package com.latteandletters.dto;

import com.latteandletters.model.Student;

public record StudentRegistrationResult(Student student,
                                        String temporaryPassword) {
}
