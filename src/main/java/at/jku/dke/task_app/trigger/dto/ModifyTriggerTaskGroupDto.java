package at.jku.dke.task_app.trigger.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying an oracle trigger task group {@link ModifyTriggerTaskGroupDto}.
 *
 * @param ddlStatements         The DDL statements for creating the tables.
 * @param diagnoseDmlStatements The DML statements for inserting the diagnose data.
 * @param submitDmlStatements   The DML statements for inserting the submission data.
 */
public record ModifyTriggerTaskGroupDto(@NotNull String ddlStatements,
                                        @NotNull String diagnoseDmlStatements,
                                        @NotNull String submitDmlStatements) implements Serializable {
}
