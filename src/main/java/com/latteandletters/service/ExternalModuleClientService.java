package com.latteandletters.service;

import com.latteandletters.dto.LibraryApiDtos.ExternalModuleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ExternalModuleClientService {

    private final RestClient restClient;
    private final String attendanceBaseUrl;
    private final String cafeBaseUrl;

    public ExternalModuleClientService(@Value("${latteandletters.integration.attendance-base-url:}") String attendanceBaseUrl,
                                       @Value("${latteandletters.integration.cafe-base-url:}") String cafeBaseUrl) {
        this.restClient = RestClient.create();
        this.attendanceBaseUrl = normalizeBaseUrl(attendanceBaseUrl);
        this.cafeBaseUrl = normalizeBaseUrl(cafeBaseUrl);
    }

    public ExternalModuleResponse attendanceStudentStatus(String studentId) {
        return get("attendance", attendanceBaseUrl, "/api/attendance/students/" + studentId + "/status");
    }

    public ExternalModuleResponse cafeStudentProfile(String studentId) {
        return get("cafe", cafeBaseUrl, "/api/cafe/students/" + studentId + "/profile");
    }

    public ExternalModuleResponse attendanceHealth() {
        return get("attendance", attendanceBaseUrl, "/api/attendance/health");
    }

    public ExternalModuleResponse cafeHealth() {
        return get("cafe", cafeBaseUrl, "/api/cafe/health");
    }

    private ExternalModuleResponse get(String module, String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return new ExternalModuleResponse(module, null, false, false, null,
                    "Base URL is not configured. Set the matching latteandletters.integration.* property.");
        }

        String url = baseUrl + path;
        try {
            ResponseEntity<Object> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(Object.class);
            return new ExternalModuleResponse(module, url, true, response.getStatusCode().is2xxSuccessful(), response.getBody(), null);
        } catch (RestClientException exception) {
            return new ExternalModuleResponse(module, url, true, false, null, exception.getMessage());
        }
    }

    private String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }
        String trimmed = rawUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
