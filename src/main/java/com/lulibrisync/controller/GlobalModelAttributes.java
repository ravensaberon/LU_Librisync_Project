package com.lulibrisync.controller;

import com.lulibrisync.service.AuthService;
import com.lulibrisync.service.AcademicProgramService;
import com.lulibrisync.util.YearLevelOptions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAttributes {

    private final AcademicProgramService academicProgramService;
    private final AuthService authService;

    public GlobalModelAttributes(AcademicProgramService academicProgramService,
                                 AuthService authService) {
        this.academicProgramService = academicProgramService;
        this.authService = authService;
    }

    @ModelAttribute
    public void populateSharedSelections(Model model) {
        model.addAttribute("programOptionsByCollege", academicProgramService.getProgramsByCollege());
        model.addAttribute("programOptionLookup", academicProgramService.getProgramOptionLookup());
        model.addAttribute("yearLevelOptions", YearLevelOptions.getOptions());
        model.addAttribute("yearLevelOptionLookup", YearLevelOptions.getOptionLookup());
        model.addAttribute("registrationCityZipCodes", authService.getLagunaCityZipCodes());
    }
}
