package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.task_app.trigger.evaluation.Snapshot.Snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecutionResult {
    private boolean successful;
    private boolean syntaxError;
    private String executionMessage;
    private List<Snapshot> executionResult;
    private Map<String, List<List<String>>> initialStates;
    private Map<String, List<String>> tableHeaders;
    private Map<String, List<List<String>>> modifiedStates;


    public ExecutionResult(boolean successful, boolean syntaxError, String executionMessage, List<Snapshot> executionResult,  Map<String, List<List<String>>> initialStates, Map<String, List<String>> tableHeaders, Map<String, List<List<String>>> modifiedStates) {
        this.successful = successful;
        this.syntaxError = syntaxError;
        this.executionMessage = removeSchema(executionMessage);
        this.executionResult = executionResult;
        this.initialStates = initialStates;
        this.tableHeaders = tableHeaders;
        this.modifiedStates = modifiedStates;
    }

    public ExecutionResult() {
        this.successful = true;
        this.executionMessage = "";
        this.executionResult = new ArrayList<>();
    }

    private String removeSchema(String executionMessage) {
        return  executionMessage.replaceAll("(?i)Submit\\d+\\.|Diagnose\\d+\\.", "");
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public boolean isSyntaxError() { return syntaxError; }

    public void setSyntaxError(boolean syntaxError) { this.syntaxError = syntaxError; }

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

    public Map<String, List<List<String>>> getInitialStates() {  return initialStates; }

    public void setInitialStates(Map<String, List<List<String>>> initialStates) { this.initialStates = initialStates; }

    public Map<String, List<String>> getTableHeaders() { return tableHeaders; }

    public void setTableHeaders(Map<String, List<String>> tableHeaders) { this.tableHeaders = tableHeaders; }

    public Map<String, List<List<String>>> getModifiedStates() { return modifiedStates; }

    public void setModifiedStates(Map<String, List<List<String>>> modifiedStates) { this.modifiedStates = modifiedStates; }
}
