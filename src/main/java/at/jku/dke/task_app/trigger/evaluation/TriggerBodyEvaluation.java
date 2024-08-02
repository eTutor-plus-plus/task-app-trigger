package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.Snapshot;
import at.jku.dke.task_app.trigger.services.DefaultTables;
import at.jku.dke.task_app.trigger.services.TriggerSchemaServiceImpl;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class TriggerBodyEvaluation {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerSchemaServiceImpl.class);

    private final TriggerTask task;
    private final SubmitSubmissionDto<TriggerSubmissionDto> submission;
    private final String userTrigger;
    private final List<CriterionDto> criterionDtos;
    private final MessageSource messageSource;
    private final int feedbackLevel;
    private final String mode;
    private final Locale locale;
    private boolean parsable;
    private final List<Snapshot> executionResult;
    private String feedbackMessage;


    public TriggerBodyEvaluation(SubmitSubmissionDto<TriggerSubmissionDto> submission, TriggerTask task, MessageSource messageSource) {
        if (submission.feedbackLevel() < 0 || submission.feedbackLevel() > 3) {
            throw new IllegalArgumentException("feedbackLevel must be between 0 and 3");
        } else {
            this.submission = submission;
            this.task = task;
            this.messageSource = messageSource;
            this.locale = Locale.of(submission.language());
            this.userTrigger = submission.submission().input();
            this.criterionDtos = new ArrayList<>();
            this.mode = submission.mode().name();
            this.parsable = true;
            this.executionResult = new ArrayList<>();
            this.feedbackLevel = mode.equals("SUBMIT") ? 0 : submission.feedbackLevel();
        }
    }

    private void addCriteria(String criteria, int showAtFeedbackLevel) {
        this.parsable = false;
        if (showAtFeedbackLevel <= this.feedbackLevel) {
            this.criterionDtos.add(new CriterionDto(
                this.messageSource.getMessage("criteria.triggerBody", null, this.locale),
                null,
                false,
                criteria));
        }
    }

    public List<CriterionDto> getCriteria() {
        return this.criterionDtos;
    }

    public String getGeneralFeedback() {
        //TODO:
        return null;
    }

    public BigDecimal getPoints() {
        if (!parsable) {
            return task.getMaxPoints().add(task.getMaxPoints().multiply(task.getWrongBodyPenalty()));
        } else {
            return task.getMaxPoints();
        }
    }

    public boolean createUserTrigger (Connection connection) {
        try {
            Statement statement = connection.createStatement();

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
            String createTriggerStatements = submission.submission().input();
            for (String createTriggerStatement : createTriggerStatements.split("/")) {
                statement.execute(createTriggerStatement);
            }
            statement.execute("COMMIT");
        }
        catch (SQLException ex) {
            if (mode.equals("RUN")) {
               feedbackMessage = ex.getMessage();
            }
            LOG.error("Failed to create trigger", ex);
            return false;
        }
        return true;
    }

    public boolean executeUserTrigger (Connection connection) {
        try {
            Statement statement = connection.createStatement();
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
                //Only one comparison after the final statement
                statement.execute("COMMIT");
                Map<String, ResultSet> tables = new HashMap<>();
                for (String resultTable : resultTables) {
                    ResultSet rs = statement.executeQuery("SELECT * FROM " + resultTable);
                    tables.put(resultTable, rs);
                }
                executionResult.add(new Snapshot(task.getId(), task.getTriggerOperations().strip(), tables));
            }
        }
        catch (SQLException ex) {
            LOG.error("Failed to create trigger", ex);
            return false;
        }
        return true;
    }

    public void resetConnection (Connection connection) {
        try {
            Statement statement = connection.createStatement();
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
        } catch (Exception ex) {
            LOG.error("SQL exception when cleaning schema", ex);
        }
    }

    public boolean compareResults(List<Snapshot> taskSolutionExecution) {
        //TODO: Mengenvergleich und Criterien speichern
        return false;
    }
}
