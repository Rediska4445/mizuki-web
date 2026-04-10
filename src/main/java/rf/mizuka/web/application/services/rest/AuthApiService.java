package rf.mizuka.web.application.services.rest;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import rf.mizuka.web.application.database.rest.repository.DeveloperRepository;
import rf.mizuka.web.application.models.rest.Developer;

import java.time.Instant;

@Service
public class AuthApiService {
    private final DeveloperRepository developerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthApiService(DeveloperRepository developerRepository,
                          PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder) {
        this.developerRepository = developerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    public String login(String clientId, String clientSecret) {
        Developer developer = developerRepository.findByClientId(clientId)
                .orElseThrow(() -> new BadCredentialsException("Invalid Client ID"));

        if (!passwordEncoder.matches(clientSecret, developer.getClientSecret())) {
            throw new BadCredentialsException("Invalid Client Secret");
        }

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600);

        String scope = String.join(" ", developer.getScopes());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(developer.getClientId())
                .claim("scp", scope)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}