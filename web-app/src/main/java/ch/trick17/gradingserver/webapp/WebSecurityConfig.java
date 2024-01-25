package ch.trick17.gradingserver.webapp;

import ch.trick17.gradingserver.webapp.model.Role;
import ch.trick17.gradingserver.webapp.model.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static ch.trick17.gradingserver.webapp.model.Role.ADMIN;
import static ch.trick17.gradingserver.webapp.model.Role.LECTURER;
import static java.util.Objects.requireNonNullElse;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserRepository userRepo;

    public WebSecurityConfig(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        var query = "(\\?.*)?"; // regex that matches arbitrary (optional) query strings
        http
                .csrf()
                    .ignoringAntMatchers("/webhooks/**")
                    .and()
                .authorizeRequests()
                    .regexMatchers(
                            "/css/.*",
                            "/favicon/.*",
                            "/" + query,
                            "/login" + query,
                            "/courses/\\d+/" + query,
                            "/courses/\\d+/problem-sets/\\d+/" + query,
                            "/courses/\\d+/problem-sets/\\d+/solutions/\\d+/submissions/\\d+/" + query,
                            "/webhooks/.*")
                        .permitAll()
                    .antMatchers("/courses/create")
                        .hasRole(LECTURER.name())
                    .antMatchers(
                            "/courses/{courseId}/edit",
                            "/courses/{courseId}/delete",
                            "/courses/{courseId}/problem-sets/add",
                            "/courses/{courseId}/problem-sets/*/register-solutions-gitlab",
                            "/courses/{courseId}/problem-sets/*/delete",
                            "/courses/{courseId}/problem-sets/*/remove-solutions",
                            "/courses/{courseId}/problem-sets/*/solutions/*/submissions/*/re-grade")
                        .access("@access.check(#courseId)")
                    .anyRequest().hasRole(ADMIN.name())
                    .and()
                .formLogin()
                    .loginPage("/login")
                    .successHandler((request, response, auth) -> {
                        var from = request.getParameter("from");
                        response.sendRedirect(requireNonNullElse(from, "/"));
                    })
                    .failureHandler((request, response, auth) -> {
                        var from = request.getParameter("from");
                        var suffix = from == null ? "" : "&from=" + from;
                        response.sendRedirect("/login?error" + suffix);
                    })
                    .permitAll()
                    .and()
                .logout()
                    .logoutSuccessHandler((request, response, auth) -> {
                        response.sendRedirect(request.getHeader("referer"));
                    })
                    .permitAll();
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return u -> userRepo.findByUsername(u)
                .orElseThrow(() -> new UsernameNotFoundException(u));
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        var hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy(Role.hierarchyString());
        return hierarchy;
    }
}
