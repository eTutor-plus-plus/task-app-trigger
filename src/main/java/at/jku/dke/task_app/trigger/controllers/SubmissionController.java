package at.jku.dke.task_app.trigger.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseSubmissionController;
import at.jku.dke.task_app.trigger.data.entities.TriggerSubmission;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import at.jku.dke.task_app.trigger.services.TriggerSubmissionService;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link TriggerSubmission}s.
 */
@RestController
public class SubmissionController extends BaseSubmissionController<TriggerSubmissionDto> {
    /**
     * Creates a new instance of class {@link SubmissionController}.
     *
     * @param submissionService The input service.
     */
    public SubmissionController(TriggerSubmissionService submissionService) {
        super(submissionService);
    }
}
