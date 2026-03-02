package at.jku.dke.task_app.trigger.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskGroupModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskGroupService;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskGroupRepository;
import at.jku.dke.task_app.trigger.dto.ModifyTriggerTaskGroupDto;
import at.jku.dke.task_app.trigger.dto.SchemaInfoDto;
import at.jku.dke.task_app.trigger.dto.TableDto;
import at.jku.dke.task_app.trigger.evaluation.ExecutionResult;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.BufferedSnapshots;
import jakarta.validation.ValidationException;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.Trigger;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Locale;

/**
 * This class provides methods for managing {@link TriggerTaskGroup}s.
 */
@Service
public class TriggerTaskGroupService extends BaseTaskGroupService<TriggerTaskGroup, ModifyTriggerTaskGroupDto> {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerDataSourceService.class);

    private final MessageSource messageSource;
    private final TriggerDataSourceService triggerDataSourceService;
    private final String sqlUrl;

    /**
     * Creates a new instance of class {@link TriggerTaskGroupService}.
     *
     * @param repository               The task group repository.
     * @param messageSource            The message source.
     * @param triggerDataSourceService The trigger datasource service.
     * @param sqlUrl                   The public SQL url.
     */
    public TriggerTaskGroupService(TriggerTaskGroupRepository repository,
                                   MessageSource messageSource, TriggerDataSourceService triggerDataSourceService,
                                   @Value("${sql-url}") String sqlUrl) {
        super(repository);
        this.messageSource = messageSource;
        this.triggerDataSourceService = triggerDataSourceService;
        this.sqlUrl = sqlUrl;
    }

    @Override
    protected TriggerTaskGroup createTaskGroup(long id, ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("trigger")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        }
        this.validateStatements(modifyTaskGroupDto);

        var taskGroup = new TriggerTaskGroup();
        taskGroup.setId(id);
        setParameters(taskGroup, modifyTaskGroupDto);
        return taskGroup;
    }

    @Override
    protected void updateTaskGroup(TriggerTaskGroup taskGroup, ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("trigger")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        }
        this.validateStatements(modifyTaskGroupDto);

        boolean dbDidNotChange = modifyTaskGroupDto.additionalData().ddlStatements().equals(taskGroup.getDdlStatements()) &&
            modifyTaskGroupDto.additionalData().diagnoseDmlStatements().equals(taskGroup.getDiagnoseDmlStatements()) &&
            modifyTaskGroupDto.additionalData().submitDmlStatements().equals(taskGroup.getSubmitDmlStatements());

        if (!dbDidNotChange) {
            BufferedSnapshots.getInstance().removeByGroupId(taskGroup.getId());
            setParameters(taskGroup, modifyTaskGroupDto);
        }
    }

    private String generateSchemaDescription(TriggerTaskGroup taskGroup) {
        if (taskGroup.getSchemaDescription() == null)
            return "";

        SchemaInfoDto dto = taskGroup.getSchemaDescription();

        StringBuilder sb = new StringBuilder("<div style=\"font-family: monospace;\">");

        // Tables
        for (var table : dto.tables()) {
            sb.append("<a target=\"_blank\" href=\"")
                .append(this.sqlUrl).append(table.queryId() != null ? table.queryId() : "")
                .append("\">").append(table.name().toUpperCase()).append("</a> (");

            for (var col : table.columns()) {
                boolean isPk = col.primaryKey();
                boolean isFk = table.foreignKeys().stream().anyMatch(f -> f.columns().contains(col.name()));

                if (isPk) sb.append("<u>");
                if (isFk) sb.append("<i>");

                sb.append(col.name());

                if (isFk) sb.append("</i>");
                if (isPk) sb.append("</u>");

                sb.append(", ");
            }

            // Remove last comma and space
            if (sb.length() >= 2)
                sb.setLength(sb.length() - 2);

            sb.append(")<br>");
        }

        sb.append("<br>");

        // Inclusions (foreign key relationships)
        for (var table : dto.tables()) {
            for (var fk : table.foreignKeys()) {
                sb.append(table.name().toUpperCase())
                    .append('(').append(String.join(", ", fk.columns())).append(") ⊆ ")
                    .append(fk.referencedTable().toUpperCase())
                    .append('(').append(String.join(", ", fk.referencedColumns())).append(") <br>");
            }
        }

        sb.append("</div>");
        return sb.toString();
    }


    private TriggerTaskGroup setParameters(TriggerTaskGroup taskGroup, ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> modifyTaskGroupDto) {
        taskGroup.setStatus(modifyTaskGroupDto.status());
        taskGroup.setDdlStatements(modifyTaskGroupDto.additionalData().ddlStatements());
        taskGroup.setDiagnoseDmlStatements(modifyTaskGroupDto.additionalData().diagnoseDmlStatements());
        taskGroup.setSubmitDmlStatements(modifyTaskGroupDto.additionalData().submitDmlStatements());
        TriggerExecutionService triggerExecutionService = new TriggerExecutionService(this.triggerDataSourceService);
        ExecutionResult submit = triggerExecutionService.validateTaskGroup(taskGroup.getDdlStatements(), taskGroup.getSubmitDmlStatements(), false);
        ExecutionResult diagnose = triggerExecutionService.validateTaskGroup(taskGroup.getDdlStatements(), taskGroup.getDiagnoseDmlStatements(), true);
        if (!submit.isSuccessful()) {
            if (submit.isSyntaxError())
            {
                throw new ValidationException("DDL statements or submission statements contain a syntax error. Error message: " + submit.getExecutionMessage());
            } else {
                throw new ValidationException("DDL statements or submission statements not valid. Error message: " + submit.getExecutionMessage());
            }
        }
        if (!diagnose.isSuccessful()) {
            if (diagnose.isSyntaxError())
            {
                throw new ValidationException("DDL statements or diagnose statements contain a syntax error. Error message: " + diagnose.getExecutionMessage());
            } else {
                throw new ValidationException("DDL statements or diagnose statements not valid. Error message: " + diagnose.getExecutionMessage());
            }
        }
        //triggerExecutionService.
            //TODO: fix
        SchemaInfoDto result = triggerExecutionService.getSchemaInfo(taskGroup.getDdlStatements());
        taskGroup.setSchemaDescription(result);
        this.repository.save(taskGroup);
        return taskGroup;
    }

    @Override
    protected TaskGroupModificationResponseDto mapToReturnData(TriggerTaskGroup taskGroup, boolean create) {
        var text = this.generateSchemaDescription(taskGroup);
        LOG.info(text);
        return new TaskGroupModificationResponseDto(
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{text}, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{text}, Locale.ENGLISH));
    }

    /**
     * Validates the statements to prevent potentially malicious code.
     *
     * @param modifyTaskGroupDto The DTO.
     * @throws jakarta.validation.ValidationException If the statements are invalid.
     */
    private void validateStatements(ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> modifyTaskGroupDto) {
        // DDL
        var tmp = modifyTaskGroupDto.additionalData().ddlStatements().toLowerCase();
        if (tmp.contains("insert into"))
            throw new ValidationException("DDL Statements must not contain INSERT INTO statements.");

        // DIAGNOSE
        tmp = modifyTaskGroupDto.additionalData().diagnoseDmlStatements().toLowerCase();
        if (tmp.contains("create table") || tmp.contains("alter table"))
            throw new ValidationException("Diagnose DML Statements must not contain CREATE or ALTER-table statements.");

        // SUBMIT
        tmp = modifyTaskGroupDto.additionalData().submitDmlStatements().toLowerCase();
        if (tmp.contains("create table") || tmp.contains("alter table"))
            throw new ValidationException("Submission DML Statements must not contain CREATE or ALTER-table statements.");
    }
}
