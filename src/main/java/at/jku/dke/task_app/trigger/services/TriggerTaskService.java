package at.jku.dke.task_app.trigger.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskInGroupService;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskGroupRepository;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskRepository;
import at.jku.dke.task_app.trigger.dto.ModifyTriggerTaskDto;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.BufferedSnapshots;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * This class provides methods for managing {@link TriggerTask}s.
 */
@Service
public class TriggerTaskService extends BaseTaskInGroupService<TriggerTask, TriggerTaskGroup, ModifyTriggerTaskDto> {

    /**
     * Creates a new instance of class {@link TriggerTaskService}.
     *
     * @param repository          The task repository.
     * @param taskGroupRepository The task group repository.
     */
    public TriggerTaskService(TriggerTaskRepository repository, TriggerTaskGroupRepository taskGroupRepository) {
        super(repository, taskGroupRepository);
    }

    @Override
    protected TriggerTask createTask(long id, ModifyTaskDto<ModifyTriggerTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("trigger"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");

        TriggerTask task = new TriggerTask();
        task.setId(id);
        task = addProperties(task, modifyTaskDto);
        return task;
    }

    @Override
    protected void updateTask(TriggerTask task, ModifyTaskDto<ModifyTriggerTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("trigger"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");

        task = addProperties(task, modifyTaskDto);
        BufferedSnapshots.getInstance().removeByTaskId(task.getId());
    }

    @Override
    protected TaskModificationResponseDto mapToReturnData(TriggerTask task, boolean create) {
        return new TaskModificationResponseDto(null, null);
    }

    private TriggerTask addProperties(TriggerTask task, ModifyTaskDto<ModifyTriggerTaskDto> modifyTaskDto) {
        task.setSolution(modifyTaskDto.additionalData().solution());
        task.setTriggerOperations(modifyTaskDto.additionalData().triggerOperations());
        task.setBuffered(modifyTaskDto.additionalData().buffered());
        task.setComparisonExecution(modifyTaskDto.additionalData().comparisonExecution());
        task.setWrongHeadPenalty(modifyTaskDto.additionalData().wrongHeadPenalty());
        task.setWrongBodyPenalty(modifyTaskDto.additionalData().wrongBodyPenalty());
        task.setResultTables(modifyTaskDto.additionalData().resultTables());
        task.setTaskGroup(this.taskGroupRepository.getReferenceById(modifyTaskDto.taskGroupId()));
        return task;
    }
}
