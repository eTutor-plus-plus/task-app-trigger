package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.task_app.trigger.evaluation.Snapshot.Snapshot;

import java.util.ArrayList;
import java.util.List;

public class ExecutionResult {
    private boolean succsfull;
    private String executionMessage;
    private List<Snapshot> executionResult;

    public ExecutionResult(boolean succsfull, String executionMessage, List<Snapshot> executionResult) {
        this.succsfull = succsfull;
        this.executionMessage = executionMessage;
        this.executionResult = executionResult;
    }

    public ExecutionResult() {
        this.succsfull = true;
        this.executionMessage = "";
        this.executionResult = new ArrayList<>();
    }

    public boolean isSuccsfull() {
        return succsfull;
    }

    public void setSuccsfull(boolean succsfull) {
        this.succsfull = succsfull;
    }

    public String getExecutionMessage() {
        return executionMessage;
    }

    public void setExecutionMessage(String executionMessage) {
        this.executionMessage = executionMessage;
    }

    public List<Snapshot> getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(List<Snapshot> executionResult) {
        this.executionResult = executionResult;
    }
}
