package at.jku.dke.task_app.trigger.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskGroupModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskGroupService;
import at.jku.dke.task_app.trigger.config.JdbcConnectionParameters;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroupQuery;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskGroupRepository;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskQueryGroupRepository;
import at.jku.dke.task_app.trigger.dto.ModifyTriggerTaskGroupDto;
import at.jku.dke.task_app.trigger.dto.SchemaInfoDto;
import at.jku.dke.task_app.trigger.dto.TableDto;
import jakarta.validation.ValidationException;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class provides methods for managing {@link TriggerTaskGroup}s.
 */
@Service
public class TriggerTaskGroupService extends BaseTaskGroupService<TriggerTaskGroup, ModifyTriggerTaskGroupDto> {

    private final MessageSource messageSource;
    private final JdbcConnectionParameters jdbcConnectionParameters;
    private final TriggerTaskQueryGroupRepository queryRepository;
    private final String sqlUrl;

    /**
     * Creates a new instance of class {@link TriggerTaskGroupService}.
     *
     * @param repository               The task group repository.
     * @param messageSource            The message source.
     * @param jdbcConnectionParameters The JDBC connection details.
     * @param queryRepository          The task group query repository.
     * @param sqlUrl                   The public SQL url.
     */
    public TriggerTaskGroupService(TriggerTaskGroupRepository repository, TriggerTaskQueryGroupRepository queryRepository,
                                   MessageSource messageSource, JdbcConnectionParameters jdbcConnectionParameters,
                                   @Value("${sql-url}") String sqlUrl) {
        super(repository);
        this.messageSource = messageSource;
        this.jdbcConnectionParameters = jdbcConnectionParameters;
        this.queryRepository = queryRepository;
        this.sqlUrl = sqlUrl;
    }

    /**
     * Creates a new schema service.
     *
     * @return Schema service
     * @throws SQLException If the connection cannot be established.
     */
    protected TriggerSchemaService createSchemaService() throws SQLException {
        return new TriggerSchemaServiceImpl(this.jdbcConnectionParameters);
    }

    //#region --- CREATE ---
    @Override
    protected TriggerTaskGroup createTaskGroup(long id, ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("trigger"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");

        this.validateStatements(modifyTaskGroupDto);

        var taskGroup = new TriggerTaskGroup();
        taskGroup.setId(id);
        taskGroup.setDdlStatements(modifyTaskGroupDto.additionalData().ddlStatements());
        taskGroup.setDiagnoseDmlStatements(modifyTaskGroupDto.additionalData().diagnoseDmlStatements());
        taskGroup.setSubmitDmlStatements(modifyTaskGroupDto.additionalData().submitDmlStatements());
        taskGroup.setSchemaName(this.buildSchemaName(id));
        return taskGroup;
    }

    protected void afterCreate(TriggerTaskGroup taskGroup, ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> dto) {
        try (var service = new TriggerSchemaServiceImpl(this.jdbcConnectionParameters)) {
            var result = service.create(taskGroup.getSchemaName(), taskGroup.getDdlStatements(), taskGroup.getDiagnoseDmlStatements(), taskGroup.getSubmitDmlStatements());
            result = this.createTaskGroupQueries(taskGroup, result);

            taskGroup.setSchemaDescription(result);
            this.repository.save(taskGroup);
        } catch (SQLException ex) {
            this.repository.delete(taskGroup);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create database tables for task group: " + ex.getMessage());
        }
    }

    /**
     * Creates task group queries for the specified schema info.
     *
     * @param taskGroup  The task group.
     * @param schemaInfo The schema info.
     * @return New schema info with task group queries.
     */
    private SchemaInfoDto createTaskGroupQueries(TriggerTaskGroup taskGroup, SchemaInfoDto schemaInfo) {
        List<TableDto> tables = new ArrayList<>();

        LOG.info("Creating task group queries for task group {}", taskGroup.getId());
        for (var table : schemaInfo.tables()) {
            var tgq = this.queryRepository.save(new TriggerTaskGroupQuery(taskGroup, table.name(), String.format("SELECT * FROM %s;", table.name())));
            tables.add(new TableDto(table.name(), table.columns(), table.foreignKeys(), tgq.getId()));
        }
        return new SchemaInfoDto(tables);
    }
    //#endregion

    //#region --- UPDATE ---
    @Override
    protected void updateTaskGroup(TriggerTaskGroup taskGroup, ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("trigger"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        this.validateStatements(modifyTaskGroupDto);

        var dbDidNotChange = modifyTaskGroupDto.additionalData().ddlStatements().equals(taskGroup.getDdlStatements()) &&
            modifyTaskGroupDto.additionalData().diagnoseDmlStatements().equals(taskGroup.getDiagnoseDmlStatements()) &&
            modifyTaskGroupDto.additionalData().submitDmlStatements().equals(taskGroup.getSubmitDmlStatements());

        if (!dbDidNotChange) {
            try (var service = this.createSchemaService()) {
                var result = service.create(taskGroup.getSchemaName(), modifyTaskGroupDto.additionalData().ddlStatements(),
                    modifyTaskGroupDto.additionalData().diagnoseDmlStatements(), modifyTaskGroupDto.additionalData().submitDmlStatements());
                result = this.updateTaskGroupQueries(taskGroup, result);
                taskGroup.setSchemaDescription(result);
            } catch (SQLException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not update database tables for task group: " + ex.getMessage());
            }
        }

        taskGroup.setDdlStatements(modifyTaskGroupDto.additionalData().ddlStatements());
        taskGroup.setDiagnoseDmlStatements(modifyTaskGroupDto.additionalData().diagnoseDmlStatements());
        taskGroup.setSubmitDmlStatements(modifyTaskGroupDto.additionalData().submitDmlStatements());
    }

    /**
     * Updates task group queries for the specified schema info.
     *
     * @param taskGroup  The task group.
     * @param schemaInfo The schema info.
     * @return New schema info with task group queries.
     */
    private SchemaInfoDto updateTaskGroupQueries(TriggerTaskGroup taskGroup, SchemaInfoDto schemaInfo) {
        List<TableDto> tables = new ArrayList<>();

        for (var table : schemaInfo.tables()) {
            var query = this.queryRepository.findByTableNameIgnoreCaseAndTaskGroup_Id(table.name(), taskGroup.getId())
                .orElseGet(() -> this.queryRepository.save(new TriggerTaskGroupQuery(taskGroup, table.name(), String.format("SELECT * FROM %s;", table.name()))));
            tables.add(new TableDto(table.name(), table.columns(), table.foreignKeys(), query.getId()));
        }

        this.queryRepository.deleteByTaskGroupAndIdNotIn(taskGroup, tables.stream().map(TableDto::queryId).toList());
        return new SchemaInfoDto(tables);
    }
    //#endregion

    //#region --- DELETE ---

    @Override
    protected void afterDelete(long id) {
        final String schemaPrefix = buildSchemaName(id);
        try (var service = this.createSchemaService()) {
            service.deleteSchemas(schemaPrefix);
            service.commit();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete database tables of task group: " + ex.getMessage());
        }
    }

    //#endregion

    //#region --- DESCRIPTION ---

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

    //#endregion

    @Override
    protected TaskGroupModificationResponseDto mapToReturnData(TriggerTaskGroup taskGroup, boolean create) {
        var text = this.generateSchemaDescription(taskGroup);
        TaskGroupModificationResponseDto result = new TaskGroupModificationResponseDto(
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{text}, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{text}, Locale.ENGLISH));
        return result;
    }

    /**
     * Builds the schema name for the specified identifier.
     *
     * @param id The task group identifier.
     * @return The schema name.
     */
    public String buildSchemaName(long id) {
        return "task_group_" + id;
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
