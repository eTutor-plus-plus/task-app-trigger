package at.jku.dke.task_app.trigger.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskGroupModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskGroupService;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskGroupRepository;
import at.jku.dke.task_app.trigger.dto.ModifyTriggerTaskGroupDto;
import at.jku.dke.task_app.trigger.dto.SchemaInfoDto;
import at.jku.dke.task_app.trigger.dto.TableDto;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.BufferedSnapshots;
import jakarta.validation.ValidationException;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

        // Parse schema info
        SchemaInfoDto dto = taskGroup.getSchemaDescription();

        // Build
        StringBuilder sb = new StringBuilder("<div style=\"font-family: monospace;\">");
        TriConsumer<StringBuilder, TableDto, String> colFunc = (s, table, col) -> {
            boolean isFk = table.foreignKeys().stream().anyMatch(f -> f.columns().contains(col));
            if (isFk)
                s.append("<i>");
            s.append(col);
            if (isFk)
                s.append("</i>");
            s.append(", ");
        };

        // Tables
        for (var table : dto.tables()) {
            sb.append("<a target=\"_blank\" href=\"");
            sb.append(this.sqlUrl).append(table.queryId()).append("\">").append(table.name()).append("</a> (");

            // PK columns
            sb.append("<u>");
            table.columns().stream().filter(TableDto.ColumnDto::primaryKey).forEach(x -> colFunc.accept(sb, table, x.name()));
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("</u>");
            if (table.columns().stream().anyMatch(x -> !x.primaryKey()))
                sb.append(", ");

            // Other columns
            table.columns().stream().filter(x -> !x.primaryKey()).forEach(x -> colFunc.accept(sb, table, x.name()));

            // Remove last comma
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append(")<br>");
        }
        sb.append("<br>");

        // Inclusions
        for (var table : dto.tables()) {
            for (var fk : table.foreignKeys()) {
                sb.append(fk.table()).append('(').append(String.join(", ", fk.columns())).append(") ⊆ ");
                sb.append(fk.referencedTable()).append('(').append(String.join(", ", fk.referencedColumns())).append(") <br>");
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
