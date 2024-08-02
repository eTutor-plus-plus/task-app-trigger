package at.jku.dke.task_app.trigger.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * This class represents a data transfer object for modifying an oracle trigger task {@link ModifyTriggerTaskDto}.
 *
 * @param solution              The sql database trigger solution.
 * @param triggerOperations     The database operations which execute the trigger.
 * @param resultTables          Tables which will be compared.
 * @param buffered              If the task solution should be buffered.
 * @param comparisonExecution   Determines if the comparison will be executed after each trigger operation.
 * @param wrongHeadPenalty  The penalty for wrong head of the trigger (-1 means full deduction of points).
 * @param wrongBodyPenalty  The penalty for wrong body of the trigger, which represents the implementation logic (-1 means full deduction of points).
 */
public record ModifyTriggerTaskDto(@NotNull String solution,
                                   @NotNull String triggerOperations,
                                   @NotNull String resultTables,
                                   @NotNull boolean buffered,
                                   @NotNull boolean comparisonExecution,
                                   @NotNull BigDecimal wrongHeadPenalty,
                                   @NotNull BigDecimal wrongBodyPenalty) implements Serializable {
}
