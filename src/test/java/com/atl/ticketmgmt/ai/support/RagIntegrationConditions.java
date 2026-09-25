package com.atl.ticketmgmt.ai.support;

import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Locale;

public final class RagIntegrationConditions {

    private static final String DEFAULT_JDBC =
            System.getenv().getOrDefault("PGVECTOR_IT_JDBC_URL", "jdbc:postgresql://localhost:5432/ticketmgmt");
    private static final String DEFAULT_USER = System.getenv().getOrDefault("PGVECTOR_IT_DB_USER", "ticket");
    private static final String DEFAULT_PASSWORD = System.getenv().getOrDefault("PGVECTOR_IT_DB_PASSWORD", "ticket");
    private static final String OLLAMA_BASE =
            System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
    private static final String EMBEDDING_MODEL =
            System.getenv().getOrDefault("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text");
    private static final String CHAT_MODEL = System.getenv().getOrDefault("OLLAMA_CHAT_MODEL", "llama3.2");

    private RagIntegrationConditions() {}

    public static boolean isEnvironmentReady() {
        return isPostgresReady() && ollamaModelsReady();
    }

    /** PostgreSQL reachable for PGVector integration tests (no Ollama required). */
    public static boolean isPostgresReady() {
        return postgresReady();
    }

    public static String readinessReport() {
        StringBuilder report = new StringBuilder();
        report.append("PostgreSQL reachable: ").append(postgresReady()).append('\n');
        report.append("Ollama models ready: ").append(ollamaModelsReady()).append('\n');
        report.append("JDBC URL: ").append(DEFAULT_JDBC).append('\n');
        report.append("Ollama base URL: ").append(OLLAMA_BASE).append('\n');
        report.append("Expected embedding model: ").append(EMBEDDING_MODEL).append('\n');
        report.append("Expected chat model: ").append(CHAT_MODEL).append('\n');
        return report.toString();
    }

    private static boolean postgresReady() {
        try (Connection connection = DriverManager.getConnection(DEFAULT_JDBC, DEFAULT_USER, DEFAULT_PASSWORD)) {
            return connection.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean ollamaModelsReady() {
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) URI.create(OLLAMA_BASE + "/api/tags").toURL().openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != 200) {
                return false;
            }
            String body = new String(connection.getInputStream().readAllBytes());
            String lower = body.toLowerCase(Locale.ROOT);
            return lower.contains(EMBEDDING_MODEL.toLowerCase(Locale.ROOT))
                    && lower.contains(CHAT_MODEL.toLowerCase(Locale.ROOT));
        } catch (Exception ex) {
            return false;
        }
    }
}
