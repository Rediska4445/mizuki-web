package rf.mizuka.web.application.configurations.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import rf.mizuka.web.application.database.repository.user.UserRepository;
import rf.mizuka.web.application.services.user.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    private final UserRepository userRepository;

    @Qualifier("bCryptPasswordEncoder")
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(UserRepository userRepository, PasswordEncoder passwordEncoder)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /*
        Main filter chain for usually users.
        Rules:
        1. Unauthorized users cannot access any service pages, except for the login page.
        2. Authorized users can access the following pages: home, tracks.
        3. All users have access to specific static assets (js, css, html, ico, img).
        4. All operations must be performed using CSRF token technology.
        5. API access is prohibited for everyone.
        6. Authorized users can terminate their session (logout).
        ---
        All necessary tests must adhere to and verify the specified rules.
    */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http)
            throws Exception
    {
        http
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                        .requireExplicitSave(false)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").denyAll()
                        .requestMatchers("/.well-known/**").denyAll()
                        .requestMatchers("/favicon.ico", "/css/**", "/js/**", "/icons/**").permitAll()
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/track/**", "/tracks/**").authenticated()
                        .requestMatchers("/audio/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/auth/login"))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .failureUrl("/auth/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    /* Developer security chain for developers and other applications */
    /* While don't touch */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http)
            throws Exception
    {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(ss -> ss.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/token").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler()
    {
        LoginUrlAuthenticationEntryPoint entryPoint =
                new LoginUrlAuthenticationEntryPoint("/auth/login");

        return (request, response, accessDeniedException) ->
        {
            if (request.getServletPath().startsWith("/api/"))
            {
                response.setStatus(HttpStatus.FORBIDDEN.value());
            }
            else
            {
                entryPoint.commence(request, response, null);
            }
        };
    }

    @Bean
    public UserDetailsService userDetailsService()
    {
        return new CustomUserDetailsService(userRepository);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService)
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider daoAuthenticationProvider)
    {
        return new ProviderManager(daoAuthenticationProvider);
    }
}