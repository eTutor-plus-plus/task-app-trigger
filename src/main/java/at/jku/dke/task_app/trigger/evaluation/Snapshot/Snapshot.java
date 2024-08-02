package at.jku.dke.task_app.trigger.evaluation.Snapshot;

import java.sql.ResultSet;
import java.util.Map;

public class Snapshot {
    private long taskId;
    private String executionStatement;
    Map<String, ResultSet> tablesSnapshot;

    public Snapshot (long taskId, String executionStatement, Map<String,ResultSet> tablesSnapshot) {
        this.taskId = taskId;
        this.executionStatement = executionStatement;
        this.tablesSnapshot = tablesSnapshot;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getExecutionStatement() {
        return executionStatement;
    }

    public void setExecutionStatement(String executionStatement) {
        this.executionStatement = executionStatement;
    }

    public Map<String, ResultSet> getTablesSnapshot() {
        return tablesSnapshot;
    }

    public void setTablesSnapshot(Map<String, ResultSet> tablesSnapshot) {
        this.tablesSnapshot = tablesSnapshot;
    }
}
