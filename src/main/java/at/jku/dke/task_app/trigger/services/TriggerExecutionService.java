package at.jku.dke.task_app.trigger.services;

import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.trigger.config.TriggerDatasource;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.dto.SchemaInfoDto;
import at.jku.dke.task_app.trigger.dto.TableDto;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import at.jku.dke.task_app.trigger.evaluation.ExecutionResult;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.Snapshot;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TriggerExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerSchemaService.class);
    private final TriggerDataSourceService dataSource;


    public TriggerExecutionService(TriggerDataSourceService dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets information about the tables.
     *
     * @return The schema information.
     * @throws SQLException If the information could not be retrieved.
     */
    public SchemaInfoDto getSchemaInfo(String ddlStm) {
        TriggerDatasource triggerDatasource = this.dataSource.getDataSource(true);
        HikariDataSource hikariDataSource = triggerDatasource.getDataSource();

        try (Connection conn = hikariDataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                //Build schema
                String ddlStatements = ddlStm.strip();
                for (String ddlStatement : ddlStatements.split(";")) {
                    statement.execute(ddlStatement);
                }
                statement.execute("COMMIT");

                //Get actual schema description
                List<TableDto> tables = new ArrayList<>();
                //Select all tables for current schema
                ResultSet rsTables = statement.executeQuery("SELECT table_name FROM user_tables");
                List<String> tableNames = new ArrayList<>();
                while (rsTables.next()) {
                    String tableName = rsTables.getString("table_name");
                    if (!EnumUtils.isValidEnum(DefaultTables.class, tableName)) {
                        tableNames.add(tableName);
                    }
                }

                //Parse all relevant tables
                for (String tableName : tableNames) {
                    // load columns
                    List<TableDto.ColumnDto> columns = new ArrayList<>();
                    ResultSet rsColumns = conn.getMetaData().getColumns(null, null, tableName, null);
                    while (rsColumns.next()) {
                        String columnName = rsColumns.getString("COLUMN_NAME");
                        String columnType = rsColumns.getString("TYPE_NAME");
                        boolean nullable = rsColumns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                        boolean pk = false;

                        // load primary keys
                        ResultSet rsPk = conn.getMetaData().getPrimaryKeys(null, null, tableName);
                        while (rsPk.next()) {
                            if (rsPk.getString("COLUMN_NAME").equals(rsColumns.getString("COLUMN_NAME"))) {
                                pk = true;
                                break;
                            }
                        }

                        // add column to list
                        columns.add(new TableDto.ColumnDto(columnName, columnType, nullable, pk));
                    }

                    // load foreign keys
                    List<TableDto.ForeignKeyDto> foreignKeys = new ArrayList<>();
                    ResultSet rsFk = conn.getMetaData().getImportedKeys(null, null, tableName);
                    while (rsFk.next()) {
                        String fkName = rsFk.getString("FK_NAME");
                        String fkTableName = rsFk.getString("FKTABLE_NAME");
                        String fkColumnName = rsFk.getString("FKCOLUMN_NAME");
                        String pkTableName = rsFk.getString("PKTABLE_NAME");
                        String pkColumnName = rsFk.getString("PKCOLUMN_NAME");

                        Optional<TableDto.ForeignKeyDto> existing = foreignKeys.stream().filter(x -> x.name().equals(fkName)).findFirst();
                        if (existing.isEmpty())
                            foreignKeys.add(new TableDto.ForeignKeyDto(fkName, fkTableName, new ArrayList<>() {{
                                add(fkColumnName);
                            }}, pkTableName, new ArrayList<>() {{
                                add(pkColumnName);
                            }}));
                        else {
                            existing.get().columns().add(fkColumnName);
                            existing.get().referencedColumns().add(pkColumnName);
                        }
                    }

                    tables.add(new TableDto(tableName, columns, foreignKeys, null));
                }
                dataSource.clear(triggerDatasource, true);
                return new SchemaInfoDto(tables);
            }
        } catch (SQLException ex) {
            LOG.error("Error when executing the trigger {}", ex.getMessage());
            //firmly clear the database, possible malicious user execution caused an error
            dataSource.clear(triggerDatasource, true);
        }
        return null;
    }

    public ExecutionResult executeTask(TriggerTask task, boolean diagnose) {
        String initStatements;
        if (diagnose) {
            initStatements = task.getTaskGroup().getDiagnoseDmlStatements();
        } else {
            initStatements = task.getTaskGroup().getSubmitDmlStatements();
        }
        return execute(
            task.getTaskGroup().getDdlStatements(),
            initStatements,
            task.getSolution(),
            task.getTriggerOperations(),
            task.getResultTables(),
            task.getId(),
            task.isComparisonExecution(),
            true,
            false,
            diagnose,
            task.getTaskGroup().getId()
        );
    }

    public ExecutionResult executeUserSubmission(TriggerTask task, SubmitSubmissionDto<TriggerSubmissionDto> submission, boolean withStatements) {
        String initStatements;
        boolean diagnose;
        if (submission.mode().name().equals("DIAGNOSE")) {
            initStatements = task.getTaskGroup().getDiagnoseDmlStatements();
            diagnose = true;
        } else {
            initStatements = task.getTaskGroup().getSubmitDmlStatements();
            diagnose = false;
        }
        return execute(
            task.getTaskGroup().getDdlStatements(),
            initStatements,
            submission.submission().input(),
            task.getTriggerOperations(),
            task.getResultTables(),
            task.getId(),
            task.isComparisonExecution(),
            withStatements,
            true,
            diagnose,
            task.getTaskGroup().getId()
        );
    }

    private ExecutionResult execute(String ddlStm, String initStm, String createStm, String executeStm, String executeOn, long taskId, boolean comparisonExecution, boolean withStatements, boolean userSubmission, boolean diagnose, long taskGroupId) {
        List<Snapshot> executionResult = new ArrayList<>();
        ExecutionResult result = new ExecutionResult();
        TriggerDatasource triggerDatasource = this.dataSource.getDataSource(diagnose);
        HikariDataSource hikariDataSource = triggerDatasource.getDataSource();
        try (Connection conn = hikariDataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                //Build schema
                String ddlStatements = ddlStm.strip();
                for (String ddlStatement : ddlStatements.split(";")) {
                    statement.execute(ddlStatement);
                }
                statement.execute("COMMIT");

                //Run schema init statements
                String initStatements = initStm.strip();
                for (String diagnoseStatement : initStatements.split(";")) {
                    statement.execute(diagnoseStatement);
                }
                statement.execute("COMMIT");

                String[] resultTables = executeOn.strip().split(";");

                Map<String, List<String>> tableHeaders = new HashMap<>();
                Map<String, List<List<String>>> initialStates = new HashMap<>();
                if(userSubmission) {
                    for (String resultTable : resultTables) {
                        tableHeaders.put(resultTable, getTableHeader(statement, resultTable));
                        initialStates.put(resultTable, getConvertedResultSet(statement, resultTable));
                    }
                }

                //Create Trigger
                String createTriggerStatements = createStm.strip();
                for (String createTriggerStatement : createTriggerStatements.split("/")) {
                    statement.execute(createTriggerStatement);
                }
                statement.execute("COMMIT");

                if (withStatements) {
                    //Execute Trigger
                    String executeTriggerStatements = executeStm.strip();
                    for (String executeTriggerStatement : executeTriggerStatements.split(";")) {
                        statement.execute(executeTriggerStatement);
                        if (comparisonExecution) {
                            //Compare after each operation
                            statement.execute("COMMIT");
                            Map<String, List<List<String>>> tables = new HashMap<>();
                            for (String resultTable : resultTables) {
                                tables.put(resultTable, getConvertedResultSet(statement, resultTable));
                            }
                            executionResult.add(new Snapshot(taskGroupId, taskId, diagnose, executeTriggerStatement, tables));
                        }
                    }
                    if (!comparisonExecution) {
                        statement.execute("COMMIT");
                        Map<String, List<List<String>>> tables = new HashMap<>();
                        for (String resultTable : resultTables) {
                            tables.put(resultTable, getConvertedResultSet(statement, resultTable));
                        }
                        executionResult.add(new Snapshot(taskGroupId, taskId, diagnose, executeTriggerStatements, tables));
                    }
                }
                Map<String, List<List<String>>> modifiedStates = new HashMap<>();
                if (userSubmission) {
                    for(String resultTable : resultTables) {
                        modifiedStates.put(resultTable, getConvertedResultSet(statement, resultTable));
                    }
                }

                dataSource.clear(triggerDatasource, diagnose);
                result = new ExecutionResult(true, false, "", executionResult, initialStates, tableHeaders, modifiedStates);
            }
        } catch (SQLException ex) {
            LOG.error("Error when executing the trigger {}", ex.getMessage());

            // Check if the exception is a syntax error, starting with "42"
            String sqlState = ex.getSQLState();
            boolean isSyntaxError = sqlState != null && sqlState.startsWith("42");

            // firmly clear the database, possible malicious user execution caused an error
            dataSource.clear(triggerDatasource, diagnose);
            result = new ExecutionResult(false, isSyntaxError, ex.getMessage(), null, null, null, null);
        }
        return result;
    }

    /**
     * Return the table header (column names) as a list.
     *
     * @param statement Statement
     * @param tableName String
     * @return List<String> where the first (and only) row contains column names
     */
    private List<String> getTableHeader(Statement statement, String tableName) throws SQLException {
        // Query to fetch metadata without returning full table
        String query = "SELECT * FROM " + tableName + " WHERE ROWNUM = 1";
        ResultSet resultSet = statement.executeQuery(query);
        ResultSetMetaData metaData = resultSet.getMetaData();

        int columnCount = metaData.getColumnCount();
        List<String> tableHeader = new ArrayList<>();

        for (int i = 1; i <= columnCount; i++) {
            tableHeader.add(metaData.getColumnName(i));
        }

        return tableHeader;
    }

    /**
     * Return a 2D list of a table order by the first column and if present then by the second column
     *
     * @param statement Statement
     * @param tableName String
     * @return CachedRowSet
     */
    private List<List<String>> getConvertedResultSet(Statement statement, String tableName) throws SQLException {
        // Query to fetch all column names from the given table
        String query = "SELECT * FROM " + tableName + " WHERE ROWNUM = 1";

        // Execute query and get the result set metadata
        ResultSet resultSet = statement.executeQuery(query);
        ResultSetMetaData metaData = resultSet.getMetaData();

        // Retrieve the number of columns
        int columnCount = metaData.getColumnCount();

        // Build the SQL query to sort by the first and second columns (if exists)
        String finalQuery = "SELECT * FROM " + tableName + " ORDER BY ";
        if (columnCount >= 1) {
            finalQuery = finalQuery + metaData.getColumnName(1) + " ASC";  // Sort by the first column
        }
        if (columnCount >= 2) {
            finalQuery = finalQuery + ", " + metaData.getColumnName(2) + " ASC";  // Sort by the second column if it exists
        }
        //Execute final query
        ResultSet rs = statement.executeQuery(finalQuery);

        //Convert table to 2D list
        List<List<String>> result = new ArrayList<>();
        addTuplesToList(rs, result);

        return result;
    }

    private void addTuplesToList(ResultSet rs, List<List<String>> tuples) throws SQLException {
        int colCount = rs.getMetaData().getColumnCount();
        while (rs.next()) {
            List<String> oneLine = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                String cell = rs.getString(i);
                if (cell == null) {
                    oneLine.add("NULL");
                } else {
                    oneLine.add(cell);
                }
            }
            tuples.add(oneLine);
        }
    }
}
