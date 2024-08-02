package at.jku.dke.task_app.trigger.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskInGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.math.BigDecimal;

/**
 * Represents an oracle trigger task.
 */
@Entity
@Table(name = "task")
public class TriggerTask extends BaseTaskInGroup<TriggerTaskGroup> {
    @Column(name = "solution", nullable = false)
    private String solution;

    @Column(name = "trigger_Operations", nullable = false)
    private String triggerOperations;

    @Column(name = "result_Tables", nullable = false)
    private String resultTables;

    @Column(name = "buffered", nullable = false)
    private boolean buffered;

    @Column(name = "comparison_Execution", nullable = false)
    private boolean comparisonExecution;

    @Column(name = "wrong_Head_Penalty", precision = 5, scale = 2, nullable = false)
    private BigDecimal wrongHeadPenalty;

    @Column(name = "wrong_Body_Penalty", precision = 5, scale = 2, nullable = false)
    private BigDecimal wrongBodyPenalty;

    /**
     * Creates a new instance of class {@link TriggerTask}.
     */
    public TriggerTask() {
    }

    /**
     * Sets the solution.
     *
     * @param solution The solution.
     */
    public void setSolution(String solution) {
        this.solution = solution;
    }

    /**
     * Gets the solution.
     *
     * @return The solution.
     */
    public String getSolution() {
        return solution;
    }

    /**
     * Sets the trigger operations.
     *
     * @param triggerOperations The trigger operations.
     */
    public void setTriggerOperations(String triggerOperations) {
        this.triggerOperations = triggerOperations;
    }

    /**
     * Gets the trigger operations.
     *
     * @return The trigger operations.
     */
    public String getTriggerOperations() {
        return triggerOperations;
    }

    /**
     * Sets the trigger operations.
     *
     * @param resultTables The trigger operations.
     */
    public void setResultTables(String resultTables) {
        this.resultTables = resultTables;
    }

    /**
     * Gets the trigger operations.
     *
     * @return The trigger operations.
     */
    public String getResultTables() {
        return resultTables;
    }

    /**
     * Gets the execution type.
     *
     * @return The execution type.
     */
    public boolean isBuffered() {
        return buffered;
    }

    /**
     * Sets the execution type.
     *
     * @param buffered The execution type.
     */
    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    /**
     * Gets the comparison execution type.
     *
     * @return The comparison execution type.
     */
    public boolean isComparisonExecution() {
        return comparisonExecution;
    }

    /**
     * Sets the comparison execution type.
     *
     * @param comparisonExecution The comparison execution type.
     */
    public void setComparisonExecution(boolean comparisonExecution) {
        this.comparisonExecution = comparisonExecution;
    }

    /**
     * Gets the wrong head penalty.
     *
     * @return The wrong head penalty.
     */
    public BigDecimal getWrongHeadPenalty() {
        return wrongHeadPenalty;
    }

    /**
     * Sets the wrong head penalty.
     *
     * @param wrongHead The wrong head penalty.
     */
    public void setWrongHeadPenalty(BigDecimal wrongHead) {
        this.wrongHeadPenalty = wrongHead;
    }

    /**
     * Gets the wrong body penalty.
     *
     * @return The wrong body penalty.
     */
    public BigDecimal getWrongBodyPenalty() {
        return wrongBodyPenalty;
    }

    /**
     * Sets the wrong body penalty.
     *
     * @param wrongBody The wrong body penalty.
     */
    public void setWrongBodyPenalty(BigDecimal wrongBody) {
        this.wrongBodyPenalty = wrongBody;
    }
}
