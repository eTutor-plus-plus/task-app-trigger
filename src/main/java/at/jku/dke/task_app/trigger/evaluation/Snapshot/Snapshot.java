package at.jku.dke.task_app.trigger.evaluation.Snapshot;

import java.util.List;
import java.util.Map;

public class Snapshot {
    private long taskGroupId;
    private long taskId;
    private boolean diagnose;
    private String executionStatement;
    Map<String, List<List<String>>> tablesSnapshot;

    public Snapshot (long taskGroupId, long taskId, boolean diagnose, String executionStatement, Map<String, List<List<String>>> tablesSnapshot) {
        this.taskGroupId = taskGroupId;
        this.taskId = taskId;
        this.diagnose = diagnose;
        this.executionStatement = executionStatement;
        this.tablesSnapshot = tablesSnapshot;
    }

    public long getTaskGroupId() { return taskGroupId; }

    public void setTaskGroupId(long taskGroupId) { this.taskGroupId = taskGroupId; }

    public boolean isDiagnose() { return diagnose; }

    public void setDiagnose(boolean diagnose) { this.diagnose = diagnose; }

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

    public Map<String, List<List<String>>> getTablesSnapshot() { return tablesSnapshot; }

    public void setTablesSnapshot(Map<String, List<List<String>>> tablesSnapshot) {
        this.tablesSnapshot = tablesSnapshot;
    }
}
