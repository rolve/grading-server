package ch.trick17.gradingserver.webapp;

import ch.trick17.gradingserver.webapp.model.Role;
import ch.trick17.gradingserver.webapp.model.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserRepository userRepo;

    public WebSecurityConfig(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf()
                    .ignoringAntMatchers("/webhooks/**")
                    .and()
                .authorizeRequests()
                    .regexMatchers(
                            "/",
                            "/css/.*",
                            "/favicon/.*",
                            "/courses/\\d+/",
                            "/courses/\\d+/problem-sets/\\d+/",
                            "/courses/\\d+/problem-sets/\\d+/solutions/\\d+/submissions/\\d+/",
                            "/webhooks/.*").permitAll()
                    .regexMatchers(
                            "/courses/create",
                            "/courses/\\d+/delete",
                            "/courses/\\d+/problem-sets/add",
                            "/courses/\\d+/problem-sets/\\d+/register-solutions-gitlab",
                            "/courses/\\d+/problem-sets/\\d+/delete",
                            "/courses/\\d+/problem-sets/\\d+/remove-solutions",
                            "/courses/\\d+/problem-sets/\\d+/solutions/\\d+/submissions/\\d+/re-grade").hasRole("LECTURER")
                    .anyRequest().hasRole("ADMIN")
                    .and()
                .formLogin()
                    .loginPage("/login")
                    .permitAll()
                    .and()
                .logout()
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
