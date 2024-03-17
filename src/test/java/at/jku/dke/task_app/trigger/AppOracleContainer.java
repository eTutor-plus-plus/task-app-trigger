package at.jku.dke.task_app.trigger;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.images.builder.Transferable;

/**
 * An Oracle container for use in unit tests.
 */
class AppOracleContainer {
    public static final String DATABASE_NAME = "test_db";
    public static final String USERNAME = "etutor_trigger_test_admin";
    public static final String PASSWORD = "strong-password";
    public static final String ETUTOR_USERNAME = "etutor_trigger_test";
    public static final String ETUTOR_PASSWORD = "etutor_trigger_pwd";

    /**
     * The singleton instance of the test database container.
     */
    public static final OracleContainer INSTANCE = new OracleContainer("gvenzl/oracle-xe:21-slim")
        .withDatabaseName(DATABASE_NAME)
        .withUsername(USERNAME)
        .withPassword(PASSWORD)
        .withCopyToContainer(Transferable.of(String.format("""
                CREATE USER %s IDENTIFIED BY "%s";
                GRANT CREATE SESSION, ALTER SESSION, CREATE SEQUENCE, CREATE SYNONYM, CREATE TABLE, CREATE VIEW, CREATE TRIGGER, CREATE PROCEDURE, CREATE TYPE TO %s;
                """, ETUTOR_USERNAME, ETUTOR_PASSWORD, ETUTOR_USERNAME)),
            "/docker-entrypoint-initdb.d/900-create_user.sql");
}
