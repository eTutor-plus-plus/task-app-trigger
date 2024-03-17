package at.jku.dke.task_app.trigger.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskController;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.dto.TriggerTaskDto;
import at.jku.dke.task_app.trigger.dto.ModifyTriggerTaskDto;
import at.jku.dke.task_app.trigger.services.TriggerTaskService;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link TriggerTask}s.
 */
@RestController
public class TaskController extends BaseTaskController<TriggerTask, TriggerTaskDto, ModifyTriggerTaskDto> {

    /**
     * Creates a new instance of class {@link TaskController}.
     *
     * @param taskService The task service.
     */
    public TaskController(TriggerTaskService taskService) {
        super(taskService);
    }

    @Override
    protected TriggerTaskDto mapToDto(TriggerTask task) {
        // TODO: return new TriggerTaskDto(task.getSolution());
        throw new NotImplementedException();
    }

}
