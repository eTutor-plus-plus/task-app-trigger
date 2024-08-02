package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.Snapshot;
import at.jku.dke.task_app.trigger.services.DefaultTables;
import at.jku.dke.task_app.trigger.services.TriggerSchemaServiceImpl;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TriggerTaskExecution {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerSchemaServiceImpl.class);
    private final TriggerDataSource dataSource;


    public TriggerTaskExecution(TriggerDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Snapshot> executeTask(TriggerTask task) {
        List<Snapshot> executionResult = new ArrayList<>();

        try (Connection conn = this.dataSource.connect()) {
            Statement statement = conn.createStatement();

            //Build schema
            String ddlStatements = task.getTaskGroup().getDdlStatements().strip();
            for (String ddlStatement : ddlStatements.split(";")) {
                statement.execute(ddlStatement);
            }
            statement.execute("COMMIT");

            //Run diagnose statements
            String diagnoseStatements = task.getTaskGroup().getDiagnoseDmlStatements().strip();
            for (String diagnoseStatement : diagnoseStatements.split(";")) {
                statement.execute(diagnoseStatement);
            }
            statement.execute("COMMIT");

            //Create Trigger
            String createTriggerStatements = task.getSolution();
            for (String createTriggerStatement : createTriggerStatements.split("/")) {
                statement.execute(createTriggerStatement);
            }
            statement.execute("COMMIT");

            //Execute Trigger
            String executeTriggerStatements = task.getTriggerOperations().strip();
            String[] resultTables = task.getResultTables().strip().split(";");
            for (String executeTriggerStatement : executeTriggerStatements.split(";")) {
                statement.execute(executeTriggerStatement);
                if (task.isComparisonExecution()) {
                    //Compare after each operation
                    statement.execute("COMMIT");
                    Map<String, ResultSet> tables = new HashMap<>();
                    for (String resultTable : resultTables) {
                        ResultSet rs = statement.executeQuery("SELECT * FROM " + resultTable);
                        tables.put(resultTable, rs);
                    }
                    executionResult.add(new Snapshot(task.getId(), executeTriggerStatement, tables));
                }
            }
            if (!task.isComparisonExecution()) {
                statement.execute("COMMIT");
                Map<String, ResultSet> tables = new HashMap<>();
                for (String resultTable : resultTables) {
                    ResultSet rs = statement.executeQuery("SELECT * FROM " + resultTable);
                    tables.put(resultTable, rs);
                }
                executionResult.add(new Snapshot(task.getId(), task.getTriggerOperations().strip(), tables));
            }
            clearSchema(task, statement);
            statement.close();
            conn.close();
            return executionResult;
        } catch (SQLException ex) {
            LOG.error("Failed to connect to database", ex);
            return null;
        }
    }

    //TODO: Umbauen auf generisch
    private void clearSchema(TriggerTask task, Statement statement) {
        try {
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
        } catch (SQLException ex) {
            LOG.error("SQL exception when cleaning schema", ex);
        }
    }
}
