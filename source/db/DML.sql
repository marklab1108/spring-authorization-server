-- Sample OAuth2 Clients for OAuth2 Flows
-- Schema: poc_spring_authorization_server
--
-- ⚠️ WARNING: These are TEST credentials only!
-- 
-- Client secrets use {noop} prefix (plain text) for testing.
-- For PRODUCTION, use BCrypt encoded passwords:
--
--   Java:
--   String encoded = new BCryptPasswordEncoder().encode("your-secret");
--   // Result: {bcrypt}$2a$10$...
--
--   Or use command line:
--   htpasswd -nbBC 10 "" your-secret | tr -d ':\n' | sed 's/$2y/$2a/'

-- Client 1: Basic client for testing (Client Credentials Flow)
INSERT INTO poc_spring_authorization_server.oauth2_registered_client (
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    scopes,
    client_settings,
    token_settings
) VALUES (
    'client-1',
    'messaging-client',
    CURRENT_TIMESTAMP,
    '{noop}secret',  -- ⚠️ TEST ONLY: Use {bcrypt}... in production
    'Messaging Client',
    'client_secret_basic,client_secret_post',
    'client_credentials',
    '',
    'message.read,message.write',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON CONFLICT (id) DO NOTHING;

-- Client 2: API client with different scopes (Client Credentials Flow)
INSERT INTO poc_spring_authorization_server.oauth2_registered_client (
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    scopes,
    client_settings,
    token_settings
) VALUES (
    'client-2',
    'api-client',
    CURRENT_TIMESTAMP,
    '{noop}api-secret',  -- ⚠️ TEST ONLY: Use {bcrypt}... in production
    'API Client',
    'client_secret_basic,client_secret_post',
    'client_credentials',
    '',
    'api.read,api.write,api.delete',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON CONFLICT (id) DO NOTHING;

-- Client 3: Web application client for Authorization Code Flow
INSERT INTO poc_spring_authorization_server.oauth2_registered_client (
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    scopes,
    client_settings,
    token_settings
) VALUES (
    'client-3',
    'client-web',
    CURRENT_TIMESTAMP,
    '{noop}web-secret',  -- ⚠️ TEST ONLY: Use {bcrypt}... in production
    'Web Application Client',
    'client_secret_basic,client_secret_post',
    'authorization_code,refresh_token',
    'http://localhost:8080/callback,http://localhost:8080/login/oauth2/code/custom,http://127.0.0.1:8080/callback',
    'openid,profile,email,read,write',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON CONFLICT (id) DO NOTHING;
