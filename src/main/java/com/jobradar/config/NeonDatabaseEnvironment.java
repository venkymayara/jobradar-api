package com.jobradar.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

public final class NeonDatabaseEnvironment implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))
                || StringUtils.hasText(environment.getProperty("DB_URL"))) {
            return;
        }

        String pooled = environment.getProperty("DATABASE_URL");
        String direct = environment.getProperty("DATABASE_URL_UNPOOLED");
        if (!StringUtils.hasText(pooled) && !StringUtils.hasText(direct)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        JdbcConnection runtime = parse(StringUtils.hasText(pooled) ? pooled : direct);
        JdbcConnection migration = StringUtils.hasText(direct) ? parse(direct) : runtime.direct();

        properties.put("DB_URL", runtime.url());
        properties.put("DB_USERNAME", runtime.username());
        properties.put("DB_PASSWORD", runtime.password());
        properties.put("spring.flyway.url", migration.url());
        properties.put("spring.flyway.user", migration.username());
        properties.put("spring.flyway.password", migration.password());
        environment.getPropertySources().addLast(new MapPropertySource("neonPostgres", properties));
    }

    private static JdbcConnection parse(String value) {
        String normalized = value.strip();
        if (normalized.length() >= 2
                && ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        try {
            URI uri = URI.create(normalized);
            String[] credentials = uri.getRawUserInfo().split(":", 2);
            if (!("postgresql".equals(uri.getScheme()) || "postgres".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawPath().length() < 2 || credentials.length != 2) {
                throw new IllegalArgumentException();
            }
            return new JdbcConnection(uri.getHost(), uri.getPort(), uri.getRawPath(),
                    decode(credentials[0]), decode(credentials[1]));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new IllegalArgumentException("Invalid Neon PostgreSQL connection URI.");
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private record JdbcConnection(String host, int port, String path, String username, String password) {

        private String url() {
            return "jdbc:postgresql://" + host + (port == -1 ? "" : ":" + port) + path
                    + "?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory&channelBinding=require";
        }

        private JdbcConnection direct() {
            return new JdbcConnection(host.replaceFirst("-pooler(?=\\.)", ""), port, path, username, password);
        }
    }
}
