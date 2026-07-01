package com.latteandletters.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.latteandletters.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/login", "/register", "/register/barangays", "/register/availability", "/register/otp-state", "/register/request-otp", "/register/verify-otp", "/register/verify", "/register/resend-otp", "/forgot-password", "/forgot-password/**", "/error", "/css/**", "/assets/**", "/api/library/**", "/api/integrations/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/student/**").hasAnyRole("STUDENT", "ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(roleBasedSuccessHandler())
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .addFilterAfter(forcePasswordChangeFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OncePerRequestFilter forcePasswordChangeFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            jakarta.servlet.FilterChain filterChain) throws ServletException, IOException {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                String path = request.getRequestURI().substring(request.getContextPath().length());
                boolean isStudentPage = path.startsWith("/student/");
                boolean isPasswordChangePage = path.startsWith("/student/password/change-temporary");

                boolean mustChangePassword = !isAdmin && isStudentPage && !isPasswordChangePage
                        && userRepository.findByEmailIgnoreCase(authentication.getName())
                        .map(com.latteandletters.model.User::isMustChangePassword)
                        .orElse(false);

                if (mustChangePassword) {
                    response.sendRedirect(request.getContextPath() + "/student/password/change-temporary");
                    return;
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

                boolean mustChangePassword = !isAdmin && userRepository.findByEmailIgnoreCase(authentication.getName())
                        .map(com.latteandletters.model.User::isMustChangePassword)
                        .orElse(false);

                String targetUrl = mustChangePassword
                        ? "/student/password/change-temporary"
                        : (isAdmin ? "/admin/dashboard" : "/student/dashboard");
                response.sendRedirect(request.getContextPath() + targetUrl);
            }
        };
    }
}
