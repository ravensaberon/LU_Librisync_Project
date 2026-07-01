package com.latteandletters.controller;

import com.latteandletters.dto.LibraryApiDtos.ExternalModuleResponse;
import com.latteandletters.service.ExternalModuleClientService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/integrations")
public class ExternalIntegrationController {

    private final ExternalModuleClientService externalModuleClientService;

    public ExternalIntegrationController(ExternalModuleClientService externalModuleClientService) {
        this.externalModuleClientService = externalModuleClientService;
    }

    @GetMapping("/health")
    public Map<String, ExternalModuleResponse> health() {
        return Map.of(
                "attendance", externalModuleClientService.attendanceHealth(),
                "cafe", externalModuleClientService.cafeHealth()
        );
    }

    @GetMapping("/attendance/students/{studentId}/status")
    public ExternalModuleResponse attendanceStudentStatus(@PathVariable String studentId) {
        return externalModuleClientService.attendanceStudentStatus(studentId);
    }

    @GetMapping("/cafe/students/{studentId}/profile")
    public ExternalModuleResponse cafeStudentProfile(@PathVariable String studentId) {
        return externalModuleClientService.cafeStudentProfile(studentId);
    }
}
