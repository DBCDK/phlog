/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU 3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.phlog;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.phlog.dto.PhLogEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PhLogIT {
    protected static final PGSimpleDataSource datasource;

    static {
        datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("phlog");
        datasource.setServerName("localhost");
        datasource.setPortNumber(getPostgresqlPort());
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
    }

    private static Map<String, String> entityManagerProperties = new HashMap<>();
    private static EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeClass
    public static void migrateDatabase() throws Exception {
        final PhLogDatabaseMigrator dbMigrator = new PhLogDatabaseMigrator(datasource);
        dbMigrator.migrate();
    }

    @BeforeClass
    public static void createEntityManagerFactory() {
        entityManagerProperties.put(JDBC_USER, datasource.getUser());
        entityManagerProperties.put(JDBC_PASSWORD, datasource.getPassword());
        entityManagerProperties.put(JDBC_URL, datasource.getUrl());
        entityManagerProperties.put(JDBC_DRIVER, "org.postgresql.Driver");
        entityManagerProperties.put("eclipselink.logging.level", "FINE");
        entityManagerFactory = Persistence.createEntityManagerFactory("phLogIT", entityManagerProperties);
    }

    @Before
    public void createEntityManager() {
        entityManager = entityManagerFactory.createEntityManager(entityManagerProperties);
    }

    @Before
    public void resetDatabase() throws SQLException {
        try (Connection conn = datasource.getConnection();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM entry");
        }
    }

    @Before
    public void populateDatabase() throws URISyntaxException {
        executeScriptResource("/populate.sql");
    }

    @After
    public void clearEntityManagerCache() {
        entityManager.clear();
        entityManager.getEntityManagerFactory().getCache().evictAll();
    }

    @Test
    public void timeOfLastModificationChangesWhenPhLogEntryIsUpdated() {
        final PhLogEntry phLogEntry = entityManager.find(PhLogEntry.class, new PhLogEntry.Key()
                .withAgencyId(123456)
                .withBibliographicRecordId("testId1"));

        assertThat("phLogEntry", phLogEntry, is(notNullValue()));

        transaction_scoped(() -> phLogEntry.withDeleted(true));

        entityManager.refresh(phLogEntry);

        assertThat("timeOfLastModification updated", phLogEntry.getTimeOfLastModification().toInstant()
                .isAfter(Instant.now().minus(Duration.ofSeconds(1))), is(true));
    }

    @Test
    public void getEntriesModifiedBetween() {
        final PhLogEntry entry2 = entityManager.find(PhLogEntry.class, new PhLogEntry.Key()
                .withAgencyId(123456)
                .withBibliographicRecordId("testId2"));
        final PhLogEntry entry4 = entityManager.find(PhLogEntry.class, new PhLogEntry.Key()
                .withAgencyId(123456)
                .withBibliographicRecordId("testId4"));

        final PhLog phLog = phLog();
        final PhLog.ResultSet<PhLogEntry> resultSet = phLog.getEntriesModifiedBetween(
                entry2.getTimeOfLastModification().toInstant(),
                entry4.getTimeOfLastModification().toInstant());
        
        final Set<String> expectedIds = Stream.of("testId2", "testId3").collect(Collectors.toSet());
        final Set<String> actualIds = new HashSet<>();
        resultSet.forEach(entry -> actualIds.add(entry.getKey().getBibliographicRecordId()));
        assertThat(actualIds, is(expectedIds));
    }

    private PhLog phLog() {
        return new PhLog(entityManager);
    }

    private <T> T transaction_scoped(CodeBlockExecution<T> codeBlock) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            return codeBlock.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            transaction.commit();
        }
    }

    private void transaction_scoped(CodeBlockVoidExecution codeBlock) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            codeBlock.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            transaction.commit();
        }
    }

    /**
     * Represents a code block execution with return value
     * @param <T> return type of the code block execution
     */
    @FunctionalInterface
    interface CodeBlockExecution<T> {
        T execute() throws Exception;
    }

    /**
     * Represents a code block execution without return value
     */
    @FunctionalInterface
    interface CodeBlockVoidExecution {
        void execute() throws Exception;
    }

    private static int getPostgresqlPort() {
        final String port = System.getProperty("postgresql.port");
        if (port != null && !port.isEmpty()) {
            return Integer.parseInt(port);
        }
        return 5432;
    }

    private static void executeScriptResource(String resourcePath) {
        final URL resource = PhLogIT.class.getResource(resourcePath);
        try {
            executeScript(new File(resource.toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void executeScript(File scriptFile) {
        try (Connection conn = datasource.getConnection()) {
            JDBCUtil.executeScript(conn, scriptFile, StandardCharsets.UTF_8.name());
        } catch (SQLException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
