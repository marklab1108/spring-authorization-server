package com.example.demo.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * Configuration for Spring Authorization Server
 * Supports OAuth2 Client Credentials Flow and JWKS endpoint
 */
@Configuration
public class AuthorizationServerConfig {

    @Value("${authorization-server.issuer}")
    private String issuer;

    private final ExternalAuthProperties externalAuthProperties;

    public AuthorizationServerConfig(ExternalAuthProperties externalAuthProperties) {
        this.externalAuthProperties = externalAuthProperties;
    }

    /**
     * Authorization Server Security Filter Chain (Spring Authorization Server 1.x API)
     * Configures OAuth2 endpoints including token and JWKS endpoints
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults())   // Enable OpenID Connect 1.0
            .authorizationEndpoint(auth -> auth.consentPage("/terms"));

        // Only cache /oauth2/authorize requests (avoid robots/error pages)
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setRequestMatcher(new AntPathRequestMatcher("/oauth2/authorize"));
        http.requestCache(cache -> cache.requestCache(requestCache));
        
        http
            // Redirect unauthenticated users hitting protected endpoints to external-login
            .exceptionHandling((exceptions) -> exceptions
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/external-login"))
            );
        
        return http.build();
    }

    /**
     * Default Security Filter Chain
     * Handles authentication for non-OAuth2 endpoints with custom external authentication
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        // Allow access to custom OAuth2 endpoints
                        .requestMatchers(
                            "/oauth2/callback",
                            "/external-login"
                        ).permitAll()
                        // Allow access to static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // Disable default form login since we use external authentication
                .formLogin(form -> form.disable());
        
        // Disable CSRF for callback endpoint
        // Reason: /oauth2/callback receives data from external authentication system via redirect,
        // which cannot include CSRF tokens. This is safe because:
        // 1. The endpoint validates session consistency (external session must match stored session)
        // 2. The callback data is signed/encoded by the external system
        // 3. Authentication state is verified before any sensitive operations
        http.csrf(csrf -> csrf.ignoringRequestMatchers(
            "/oauth2/callback"
        ));
        
        return http.build();
    }

    /**
     * Shared HttpSessionRequestCache bean
     * Used by multiple controllers to access saved OAuth2 authorization requests
     */
    @Bean
    public HttpSessionRequestCache httpSessionRequestCache() {
        return new HttpSessionRequestCache();
    }

    /**
     * RestTemplate for external API calls
     * Configured with connection and read timeouts from ExternalAuthProperties
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(externalAuthProperties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(externalAuthProperties.getReadTimeoutMs()))
                .build();
    }

    /**
     * Registered Client Repository
     * Uses JDBC to store and retrieve OAuth2 client registrations
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     * OAuth2 Authorization Service
     * Uses JDBC to store and retrieve authorizations
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    // Note: OAuth2AuthorizationConsentService is provided by AuditableConsentService
    // which ensures consent is required for every authorization request and
    // records consent history for auditing purposes.

    /**
     * JWK Source for token signing
     * Generates RSA key pair for JWT signing
     * The public key is exposed via JWKS endpoint at /.well-known/jwks.json
     * 
     * NOTE: Keys are regenerated on each restart. For production, persist keys to KeyStore.
     * See README.md for planned improvements.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * Generate RSA Key Pair
     */
    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    /**
     * JWT Decoder
     * Decodes and validates JWT tokens
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Authorization Server Settings
     * Configures the issuer URL and endpoints
     * 
     * Note: Authorization requests start at the standard /oauth2/authorize endpoint.
     * Unauthenticated users are redirected to /external-login for external authentication, then continue the flow.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                // Keep standard OAuth2 endpoint paths
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .build();
    }
}
