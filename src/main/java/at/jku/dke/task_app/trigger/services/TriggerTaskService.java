package at.jku.dke.task_app.trigger.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskInGroupService;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskGroupRepository;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskRepository;
import at.jku.dke.task_app.trigger.dto.ModifyTriggerTaskDto;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * This class provides methods for managing {@link TriggerTask}s.
 */
@Service
public class TriggerTaskService extends BaseTaskInGroupService<TriggerTask, TriggerTaskGroup, ModifyTriggerTaskDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link TriggerTaskService}.
     *
     * @param repository          The task repository.
     * @param taskGroupRepository The task group repository.
     * @param messageSource       The message source.
     */
    public TriggerTaskService(TriggerTaskRepository repository, TriggerTaskGroupRepository taskGroupRepository, MessageSource messageSource) {
        super(repository, taskGroupRepository);
        this.messageSource = messageSource;
    }

    @Override
    protected TriggerTask createTask(long id, ModifyTaskDto<ModifyTriggerTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("trigger"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        // TODO: create task with data of data
        return new TriggerTask();
    }

    @Override
    protected void updateTask(TriggerTask task, ModifyTaskDto<ModifyTriggerTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("trigger"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");

        // TODO: update task properties with data of data
    }

    @Override
    protected TaskModificationResponseDto mapToReturnData(TriggerTask task, boolean create) {
        return new TaskModificationResponseDto(null, null);
    }
}
