package at.jku.dke.task_app.trigger.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskGroupModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskGroupService;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskGroupRepository;
import at.jku.dke.task_app.trigger.dto.ModifyTriggerTaskGroupDto;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * This class provides methods for managing {@link TriggerTaskGroup}s.
 */
@Service
public class TriggerTaskGroupService extends BaseTaskGroupService<TriggerTaskGroup, ModifyTriggerTaskGroupDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link TriggerTaskGroupService}.
     *
     * @param repository    The task group repository.
     * @param messageSource The message source.
     */
    public TriggerTaskGroupService(TriggerTaskGroupRepository repository, MessageSource messageSource) {
        super(repository);
        this.messageSource = messageSource;
    }

    @Override
    protected TriggerTaskGroup createTaskGroup(long id, ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("trigger"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");

        // TODO: create task group with data of data
        return new TriggerTaskGroup();
    }

    @Override
    protected void updateTaskGroup(TriggerTaskGroup taskGroup, ModifyTaskGroupDto<ModifyTriggerTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("trigger"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");

        // TODO: update task group properties with data of data
    }

    @Override
    protected TaskGroupModificationResponseDto mapToReturnData(TriggerTaskGroup taskGroup, boolean create) {
        return new TaskGroupModificationResponseDto(null, null);
    }
}
