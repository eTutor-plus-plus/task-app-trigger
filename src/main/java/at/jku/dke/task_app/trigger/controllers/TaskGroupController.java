package at.jku.dke.task_app.trigger.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskGroupController;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import at.jku.dke.task_app.trigger.dto.ModifyTriggerTaskGroupDto;
import at.jku.dke.task_app.trigger.dto.TriggerTaskGroupDto;
import at.jku.dke.task_app.trigger.services.TriggerTaskGroupService;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link TriggerTaskGroup}s.
 */
@RestController
public class TaskGroupController extends BaseTaskGroupController<TriggerTaskGroup, TriggerTaskGroupDto, ModifyTriggerTaskGroupDto> {

    /**
     * Creates a new instance of class {@link TaskGroupController}.
     *
     * @param taskGroupService The task group service.
     */
    public TaskGroupController(TriggerTaskGroupService taskGroupService) {
        super(taskGroupService);
    }

    @Override
    protected TriggerTaskGroupDto mapToDto(TriggerTaskGroup taskGroup) {
        return  new TriggerTaskGroupDto(
            taskGroup.getDdlStatements(),
            taskGroup.getDiagnoseDmlStatements(),
            taskGroup.getSubmitDmlStatements()
        );
    }
}
