package ch.trick17.gradingserver.webapp;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                    .regexMatchers("/",
                            "/css/.*",
                            "/favicon/.*",
                            "/courses/\\d+/",
                            "/courses/\\d+/problem-sets/\\d+/",
                            "/courses/\\d+/problem-sets/\\d+/solutions/\\d+/submissions/\\d+/").permitAll()
                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .loginPage("/login")
                    .permitAll()
                    .and()
                .logout()
                    .permitAll();
    }
}
