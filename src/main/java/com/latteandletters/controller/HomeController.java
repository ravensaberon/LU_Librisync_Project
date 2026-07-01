package com.latteandletters.controller;

import com.latteandletters.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin && mustChangePassword(authentication)) {
            return "redirect:/student/password/change-temporary";
        }
        return isAdmin ? "redirect:/admin/dashboard" : "redirect:/student/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin && mustChangePassword(authentication)) {
            return "redirect:/student/password/change-temporary";
        }
        return isAdmin ? "redirect:/admin/profile" : "redirect:/student/profile";
    }

    private boolean mustChangePassword(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(com.latteandletters.model.User::isMustChangePassword)
                .orElse(false);
    }
}
