package at.jku.dke.task_app.trigger.services;

import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.etutor.task_app.services.BaseSubmissionService;
import at.jku.dke.task_app.trigger.data.entities.TriggerSubmission;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.data.repositories.TriggerSubmissionRepository;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskRepository;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import at.jku.dke.task_app.trigger.evaluation.EvaluationService;
import org.springframework.stereotype.Service;

/**
 * This class provides methods for managing {@link TriggerSubmission}s.
 */
@Service
public class TriggerSubmissionService extends BaseSubmissionService<TriggerTask, TriggerSubmission, TriggerSubmissionDto> {

    private final EvaluationService evaluationService;

    /**
     * Creates a new instance of class {@link TriggerSubmissionService}.
     *
     * @param submissionRepository The input repository.
     * @param taskRepository       The task repository.
     * @param evaluationService    The evaluation service.
     */
    public TriggerSubmissionService(TriggerSubmissionRepository submissionRepository, TriggerTaskRepository taskRepository, EvaluationService evaluationService) {
        super(submissionRepository, taskRepository);
        this.evaluationService = evaluationService;
    }

    @Override
    protected TriggerSubmission createSubmissionEntity(SubmitSubmissionDto<TriggerSubmissionDto> submitSubmissionDto) {
        // TODO: create submission with data from dto
        return new TriggerSubmission();
    }

    @Override
    protected GradingDto evaluate(SubmitSubmissionDto<TriggerSubmissionDto> submitSubmissionDto) {
        return this.evaluationService.evaluate(submitSubmissionDto);
    }

    @Override
    protected TriggerSubmissionDto mapSubmissionToSubmissionData(TriggerSubmission submission) {
        // TODO: create DTO with data of submission
        return new TriggerSubmissionDto("TODO");
    }

}
