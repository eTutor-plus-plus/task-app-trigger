package at.jku.dke.task_app.trigger.services;

import at.jku.dke.task_app.trigger.config.JdbcConnectionParameters;
import at.jku.dke.task_app.trigger.config.TriggerDatasource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a connection to the exercise database for analyzing trigger queries.
 */
@Service
public class TriggerDataSourceService {
    private static final Logger LOG = LoggerFactory.getLogger(TriggerDataSourceService.class);

    private final HikariDataSource rootDataSource;
    private final List<TriggerDatasource> submitDatasources;
    private final List<TriggerDatasource> diagnoseDatasources;
    private int submitCounter = 0;
    private int diagnoseCounter = 0;

    /**
     * Creates a new instance of class {@link TriggerDataSourceService}.
     *
     * @param parameters The JDBC connection parameters.
     */
    public TriggerDataSourceService(JdbcConnectionParameters parameters) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(parameters.url());
        config.setUsername(parameters.admin().username());
        config.setPassword(parameters.admin().password());
        config.setMaximumPoolSize(parameters.maxPoolSize());
        config.setMaxLifetime(parameters.maxLifetime());
        config.setConnectionTimeout(parameters.connectionTimeout());
        config.setPoolName("jdbc-executor-pool");
        config.setReadOnly(false);
        config.setAutoCommit(false);
        config.addDataSourceProperty("ApplicationName", "etutor Task-App: TRIGGER");
        this.rootDataSource = new HikariDataSource(config);
        this.submitDatasources = new ArrayList<>();
        this.diagnoseDatasources = new ArrayList<>();

        try {
            //read the file from the classpath
            ClassPathResource resource = new ClassPathResource("db/users/dbUserConfig.json");

            //read the file into string
            String jsonString = new String(Files.readAllBytes(Paths.get(resource.getURI())));

            //parse as array
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                // Access the fields inside each JSONObject
                String username = jsonObject.getString("username");
                String password = jsonObject.getString("password");
                int maxPoolSize = jsonObject.getInt("max-pool-size");
                long maxLifetime = jsonObject.getLong("max-lifetime");
                int connectionTimeout = jsonObject.getInt("connection-timeout");
                String jdbcUrl = jsonObject.getString("jdbcUrl");

                //create new config for each entry
                HikariConfig userConfig = new HikariConfig();
                userConfig.setJdbcUrl(jdbcUrl);
                userConfig.setUsername(username);
                userConfig.setPassword(password);
                userConfig.setMaximumPoolSize(maxPoolSize);
                userConfig.setMaxLifetime(maxLifetime);
                userConfig.setConnectionTimeout(connectionTimeout);
                userConfig.setPoolName("user-pool-" + username);
                userConfig.setReadOnly(false);
                userConfig.setAutoCommit(false);
                userConfig.addDataSourceProperty("ApplicationName", "user-" + username);
                //add entry to list
                LOG.info("\"" + username + "\" , \"" + password + "\"");
                if(username.contains("diagnose")) {
                    this.diagnoseDatasources.add(new TriggerDatasource(new HikariDataSource(userConfig)));
                } else {
                    this.submitDatasources.add(new TriggerDatasource(new HikariDataSource(userConfig)));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a new database connection.
     *
     * @return The database connection.
     * @throws SQLException If the connection could not be established.
     */
    public TriggerDatasource getDataSource(boolean diagnose) {
        TriggerDatasource dataSource = null;
        if(diagnose) {
            while(dataSource == null) {
                if(diagnoseDatasources.get(diagnoseCounter).isLock()) {
                    diagnoseCounter = (diagnoseCounter + 1) % this.diagnoseDatasources.size();
                } else {
                    diagnoseDatasources.get(diagnoseCounter).setLock(true);
                    dataSource = diagnoseDatasources.get(diagnoseCounter);
                    diagnoseCounter = (diagnoseCounter + 1) % this.diagnoseDatasources.size();
                }
            }
        } else {
            while(dataSource == null) {
                if(submitDatasources.get(submitCounter).isLock()) {
                    submitCounter = (submitCounter + 1) % this.submitDatasources.size();
                } else {
                    submitDatasources.get(submitCounter).setLock(true);
                    dataSource = submitDatasources.get(submitCounter);
                    submitCounter = (submitCounter + 1) % this.submitDatasources.size();
                }
            }
        }
        return dataSource;
    }

    public void logConn() {
        HikariPoolMXBean poolProxy = rootDataSource.getHikariPoolMXBean();
        LOG.info("Total connections: {}", poolProxy.getTotalConnections());
        LOG.info("Idle connections: {}", poolProxy.getIdleConnections());
        LOG.info("Active connections: {}", poolProxy.getActiveConnections());
    }

    /**
     * Clears the datasource.
     */
    public void clear(TriggerDatasource dataSource, boolean diagnose, boolean userSubmission) {
        if(userSubmission) {
            //Possible difference between clearing a user input and conventionally clearing a schema
            //purgeSchema();
            clearSchema(dataSource);
        } else {
            clearSchema(dataSource);
        }
        unlock(dataSource, diagnose);
    }

    private void unlock(TriggerDatasource dataSource, boolean diagnose) {
        if(diagnose) {
            for(int i = 0; i < diagnoseDatasources.size(); i++) {
                TriggerDatasource index = diagnoseDatasources.get(i);
                if(index == dataSource) {
                    diagnoseDatasources.get(i).setLock(false);
                    return;
                }
            }
        } else {
            for(int i = 0; i < submitDatasources.size(); i++) {
                TriggerDatasource index = submitDatasources.get(i);
                if(index == dataSource) {
                    submitDatasources.get(i).setLock(false);
                    return;
                }
            }
        }
    }

    /**
     * Clears schema, only for use after solution execution
     */
    private void clearSchema(TriggerDatasource dataSource) {
        try (Connection conn = dataSource.getDataSource().getConnection()) {
            try (Statement statement = conn.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT table_name FROM user_tables");
                List<String> tableNames = new ArrayList<>();
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    if (!EnumUtils.isValidEnum(DefaultTables.class, tableName)) {
                        tableNames.add(tableName);
                    }
                }
                for (String tableName : tableNames) {
                    statement.execute("DROP TABLE " + tableName + " CASCADE CONSTRAINTS");
                }
                statement.execute("COMMIT");
            }
        } catch (SQLException ex) {
            LOG.error("SQL exception when cleaning schema", ex);
        }
    }

    /**
     * Purges schema, recommended for use after user execution
     */
    private void purgeSchema() {
        //TODO: purge schema with all possible user modifications
    }
}
