package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.security.AdminAuthFilter;
import br.com.synki.nfse.portal.security.AuthRateLimitFilter;
import br.com.synki.nfse.portal.security.EmbedAuthFilter;
import br.com.synki.nfse.portal.security.EmbedTokenService;
import br.com.synki.nfse.portal.security.MembershipAuthFilter;
import br.com.synki.nfse.portal.security.UsuarioActiveAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            EmbedTokenService tokenService,
            AdminAuthFilter adminAuthFilter,
            AuthRateLimitFilter authRateLimitFilter,
            UsuarioActiveAuthFilter usuarioActiveAuthFilter,
            MembershipAuthFilter membershipAuthFilter,
            PortalProperties props) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsSource(props)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/billing/stripe/webhook").permitAll()
                        .requestMatchers("/api/admin/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/nfse/servicos", "/api/nfse/nbs", "/api/nfse/cnae").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(adminAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new EmbedAuthFilter(tokenService), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(usuarioActiveAuthFilter, EmbedAuthFilter.class)
                .addFilterAfter(membershipAuthFilter, UsuarioActiveAuthFilter.class);
        return http.build();
    }

    private CorsConfigurationSource corsSource(PortalProperties props) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(props.corsOrigins().split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
