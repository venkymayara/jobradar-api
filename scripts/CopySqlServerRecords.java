import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/** Copies application records into a fresh Flyway-initialized PostgreSQL database. */
class CopySqlServerRecords {
    private static final List<String> TABLES = List.of(
            "app_roles", "categories", "companies", "external_job_provider_status", "external_jobs",
            "job_roles", "locations", "moderator_permission_history", "notifications", "opportunities",
            "opportunity_status_history", "opportunityreports", "opportunityroles", "opportunityskills",
            "savedopportunities", "skills", "user_roles", "users", "walkindetails"
    );
    private static final Set<String> SEEDED_ROLES = Set.of(
            "ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"
    );

    public static void main(String[] args) throws Exception {
        Properties env = new Properties();
        if (args.length != 1) {
            throw new IllegalArgumentException("Pass the path to the Neon environment file.");
        }
        try (var reader = Files.newBufferedReader(Path.of(args[0]))) {
            env.load(reader);
        }
        String sourceUrl = System.getenv().getOrDefault("JOBRADAR_SOURCE_JDBC_URL",
                "jdbc:sqlserver://localhost:1433;databaseName=ven_db;encrypt=true;"
                        + "trustServerCertificate=true;integratedSecurity=true;applicationIntent=ReadOnly");
        URI targetUri = directUri(env);

        try (var source = DriverManager.getConnection(sourceUrl);
             var target = openTarget(targetUri);
             var control = target.createStatement()) {
            source.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            source.setAutoCommit(false);
            target.setAutoCommit(false);
            try {
                control.execute("SET LOCAL lock_timeout = '15s'");
                control.execute("LOCK TABLE " + "dbo." + String.join(", dbo.", TABLES)
                        + " IN SHARE ROW EXCLUSIVE MODE");
                control.execute("SET CONSTRAINTS ALL DEFERRED");
                requireFreshTarget(control);

                long total = 0;
                for (String table : TABLES) {
                    int copied = copyTable(source, target, table);
                    long verified = count(control, table);
                    if (verified != copied) {
                        throw new SQLException("Record count mismatch for " + table);
                    }
                    total += verified;
                    System.out.printf("%s %d verified%n", table, verified);
                }
                control.execute("SET CONSTRAINTS ALL IMMEDIATE");
                restartIdentities(control);
                target.commit();
                System.out.printf("Committed %,d records across %d tables.%n", total, TABLES.size());
            } catch (SQLException failure) {
                target.rollback();
                System.err.printf("Copy rolled back (SQL state %s).%n", failure.getSQLState());
                throw new SQLException("Database copy failed: " + failure.getClass().getSimpleName(),
                        failure.getSQLState());
            }
        }
    }

    private static URI directUri(Properties env) {
        try {
            String value = env.getProperty("DATABASE_URL_UNPOOLED").strip();
            if (value.startsWith("\"") || value.startsWith("'")) {
                value = value.substring(1, value.length() - 1);
            }
            URI uri = URI.create(value);
            if (!"postgresql".equals(uri.getScheme()) || uri.getHost() == null
                    || uri.getHost().contains("-pooler") || uri.getRawUserInfo() == null) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new IllegalArgumentException("A valid DATABASE_URL_UNPOOLED is required.");
        }
    }

    private static Connection openTarget(URI uri) throws SQLException {
        String[] credentials = uri.getRawUserInfo().split(":", 2);
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        return DriverManager.getConnection("jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath()
                        + "?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory&channelBinding=require",
                decode(credentials[0]), decode(credentials[1]));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static void requireFreshTarget(Statement control) throws SQLException {
        for (String table : TABLES) {
            if (!"app_roles".equals(table) && count(control, table) != 0) {
                throw new SQLException("The target already contains records in " + table);
            }
        }
        try (var rows = control.executeQuery("SELECT role FROM dbo.app_roles")) {
            while (rows.next()) {
                if (!SEEDED_ROLES.contains(rows.getString(1))) {
                    throw new SQLException("The target contains application roles outside the baseline");
                }
            }
        }
    }

    private static long count(Statement control, String table) throws SQLException {
        try (var rows = control.executeQuery("SELECT count(*) FROM dbo." + table)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private static int copyTable(Connection source, Connection target, String table) throws SQLException {
        String query = "SELECT * FROM dbo." + table;
        if ("user_roles".equals(table)) {
            query = """
                    SELECT r.id, u.username, r.role
                    FROM dbo.user_roles r
                    JOIN dbo.users u ON u.username = r.username
                    """;
        }

        try (var read = source.createStatement(); var rows = read.executeQuery(query)) {
            ResultSetMetaData columns = rows.getMetaData();
            var names = new ArrayList<String>();
            var parameters = new ArrayList<String>();
            for (int i = 1; i <= columns.getColumnCount(); i++) {
                String name = columns.getColumnLabel(i).toLowerCase(Locale.ROOT);
                if (!name.matches("[a-z_][a-z0-9_]*")) {
                    throw new SQLException("Unsupported source column name");
                }
                names.add(name);
                parameters.add("?");
            }
            String insert = "INSERT INTO dbo." + table + " (" + String.join(", ", names)
                    + ") VALUES (" + String.join(", ", parameters) + ")";
            if ("app_roles".equals(table)) {
                insert += " ON CONFLICT (role) DO NOTHING";
            }

            int copied = 0;
            try (var write = target.prepareStatement(insert)) {
                while (rows.next()) {
                    for (int i = 1; i <= names.size(); i++) {
                        String type = columns.getColumnTypeName(i);
                        if ("datetimeoffset".equalsIgnoreCase(type)) {
                            write.setObject(i, rows.getObject(i, OffsetDateTime.class));
                        } else if ("uniqueidentifier".equalsIgnoreCase(type)) {
                            String id = rows.getString(i);
                            write.setObject(i, id == null ? null : UUID.fromString(id));
                        } else if (columns.getColumnType(i) == Types.TIME) {
                            write.setObject(i, rows.getObject(i, java.time.LocalTime.class));
                        } else {
                            write.setObject(i, rows.getObject(i));
                        }
                    }
                    write.addBatch();
                    copied++;
                    if (copied % 200 == 0) {
                        write.executeBatch();
                    }
                }
                write.executeBatch();
            } catch (SQLException failure) {
                System.err.println("Database write failed for table " + table);
                throw failure;
            }
            return copied;
        }
    }

    private static void restartIdentities(Statement control) throws SQLException {
        var tables = new ArrayList<String>();
        try (var rows = control.executeQuery("""
                SELECT table_name FROM information_schema.columns
                WHERE table_schema = 'dbo' AND column_name = 'id' AND is_identity = 'YES'
                """)) {
            while (rows.next()) {
                String table = rows.getString(1);
                if (TABLES.contains(table)) {
                    tables.add(table);
                }
            }
        }
        for (String table : tables) {
            long next;
            try (var rows = control.executeQuery("SELECT coalesce(max(id), 0) + 1 FROM dbo." + table)) {
                rows.next();
                next = rows.getLong(1);
            }
            control.execute("ALTER TABLE dbo." + table + " ALTER COLUMN id RESTART WITH " + next);
        }
    }
}
