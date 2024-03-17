package at.jku.dke.task_app.trigger;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures the database connection.
 */
public class DatabaseSetupExtension implements BeforeAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseSetupExtension.class);

    /**
     * Creates a new instance of class {@link DatabaseSetupExtension}.
     */
    public DatabaseSetupExtension() {
    }

    /**
     * Starts the database container and updates the database connection properties.
     *
     * @param extensionContext The extension context.
     */
    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        LOG.info("Starting database container...");
        AppOracleContainer.INSTANCE.start();
        this.updateDataSourceProps();
    }

    private void updateDataSourceProps() {
        System.setProperty("spring.test.database.replace", "none"); // Tells Spring Boot not to start in-memory db for tests.

        System.setProperty("spring.datasource.url", AppOracleContainer.INSTANCE.getJdbcUrl());
        System.setProperty("spring.datasource.username", AppOracleContainer.ETUTOR_USERNAME);
        System.setProperty("spring.datasource.password", AppOracleContainer.ETUTOR_PASSWORD);

        System.setProperty("spring.flyway.user", AppOracleContainer.INSTANCE.getUsername());
        System.setProperty("spring.flyway.password", AppOracleContainer.INSTANCE.getPassword());
    }

}
