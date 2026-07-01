package com.latteandletters.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AcademicProgramService {

    private final Map<String, List<String>> programsByCollege;
    private final Map<String, Boolean> programOptionLookup;

    public AcademicProgramService() {
        Map<String, List<String>> configuredPrograms = new LinkedHashMap<>();
        configuredPrograms.put("College of Arts and Sciences", List.of(
                "Bachelor of Arts in Communication",
                "Bachelor of Science in Psychology",
                "Bachelor of Arts in Psychology"
        ));
        configuredPrograms.put("College of Business, Administration and Accountancy", List.of(
                "Bachelor of Science in Accountancy",
                "Bachelor of Science in Accounting Information System",
                "Bachelor of Science in Entrepreneurship",
                "Bachelor of Science in Tourism Management"
        ));
        configuredPrograms.put("College of Computing Studies", List.of(
                "Bachelor of Science in Computer Science",
                "Bachelor of Science in Information Technology"
        ));
        configuredPrograms.put("College of Education", List.of(
                "Bachelor of Elementary Education",
                "Bachelor of Secondary Education"
        ));
        configuredPrograms.put("College of Engineering", List.of(
                "Bachelor of Science in Mechanical Engineering"
        ));

        this.programsByCollege = configuredPrograms.entrySet().stream()
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), List.copyOf(entry.getValue())),
                        LinkedHashMap::putAll);

        Map<String, Boolean> supportedPrograms = new LinkedHashMap<>();
        this.programsByCollege.values().forEach(programs ->
                programs.forEach(program -> supportedPrograms.put(program, Boolean.TRUE)));
        this.programOptionLookup = Map.copyOf(supportedPrograms);
    }

    public Map<String, List<String>> getProgramsByCollege() {
        return programsByCollege;
    }

    public Map<String, Boolean> getProgramOptionLookup() {
        return programOptionLookup;
    }
}
