package ch.trick17.gradingserver;

import ch.trick17.gradingserver.model.Role;
import ch.trick17.gradingserver.model.UserRepository;
import ch.trick17.gradingserver.service.AccessController;
import jakarta.servlet.ServletContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

import static ch.trick17.gradingserver.model.Role.ADMIN;
import static ch.trick17.gradingserver.model.Role.LECTURER;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

@Configuration
public class WebSecurityConfig {

    private final UserRepository userRepo;
    private final AccessController access;
    private final ServletContext context;

    public WebSecurityConfig(UserRepository userRepo, AccessController access,
                             ServletContext context) {
        this.userRepo = userRepo;
        this.access = access;
        this.context = context;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var query = "(\\?.*)?"; // regex that matches arbitrary (optional) query strings
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                regexMatcher("/css/.*"),
                                regexMatcher("/favicon/.*"),
                                regexMatcher("/" + query),
                                regexMatcher("/login" + query),
                                regexMatcher("/courses/\\d+/" + query),
                                regexMatcher("/courses/\\d+/problem-sets/\\d+/" + query),
                                regexMatcher("/courses/\\d+/problem-sets/\\d+/solutions/\\d+/submissions/\\d+/" + query),
                                regexMatcher("/webhooks/.*")).permitAll()
                        .requestMatchers("/courses/create").hasRole(LECTURER.name())
                        .requestMatchers(
                                "/courses/{courseId}/edit",
                                "/courses/{courseId}/delete",
                                "/courses/{courseId}/problem-sets/add",
                                "/courses/{courseId}/problem-sets/*/register-solutions-gitlab",
                                "/courses/{courseId}/problem-sets/*/edit",
                                "/courses/{courseId}/problem-sets/*/delete",
                                "/courses/{courseId}/problem-sets/*/remove-solutions",
                                "/courses/{courseId}/problem-sets/*/solutions/*/submissions/*/re-grade")
                        .access(access.writeAccessChecker())
                        .anyRequest().hasRole(ADMIN.name()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/webhooks/**"))
                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler((request, response, auth) -> {
                            var from = request.getParameter("from");
                            var location = context.getContextPath() + requireNonNullElse(from, "/");
                            response.sendRedirect(location);
                        })
                        .failureHandler((request, response, auth) -> {
                            var from = request.getParameter("from");
                            var suffix = from == null ? "" : "&from=" + from;
                            response.sendRedirect(context.getContextPath() + "/login?error" + suffix);
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, auth) -> {
                            response.sendRedirect(request.getHeader("referer"));
                        })
                        .permitAll())
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return u -> userRepo.findByUsername(u)
                .orElseThrow(() -> new UsernameNotFoundException(u));
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(Role.hierarchyString());
    }
}
