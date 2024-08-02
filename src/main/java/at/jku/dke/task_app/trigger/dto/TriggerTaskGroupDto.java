package at.jku.dke.task_app.trigger.dto;

import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link TriggerTaskGroup}
 *
 * @param ddlStatements         The DDL statements for creating the tables.
 * @param diagnoseDmlStatements The DML statements for inserting the diagnose data.
 * @param submitDmlStatements   The DML statements for inserting the submission data.
 */
public record TriggerTaskGroupDto(@NotNull String ddlStatements,
                                  @NotNull String diagnoseDmlStatements,
                                  @NotNull String submitDmlStatements) implements Serializable {
}
