package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskRepository;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.BufferedSnapshots;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.Snapshot;
import at.jku.dke.task_app.trigger.services.TriggerDataSourceService;
import at.jku.dke.task_app.trigger.services.TriggerExecutionService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service that evaluates submissions.
 */
@Service
public class EvaluationService {
    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);

    private final TriggerTaskRepository taskRepository;
    private final MessageSource messageSource;
    private final TriggerDataSourceService triggerDataSourceService;

    /**
     * Creates a new instance of class {@link EvaluationService}.
     *
     * @param taskRepository    The task repository.
     * @param messageSource     The message source.
     * @param triggerDataSourceService The datasource.
     */
    public EvaluationService(TriggerTaskRepository taskRepository, MessageSource messageSource, TriggerDataSourceService triggerDataSourceService) {
        this.taskRepository = taskRepository;
        this.messageSource = messageSource;
        this.triggerDataSourceService = triggerDataSourceService;
    }

    /**
     * Evaluates a input.
     *
     * @param submission The input to evaluate.
     * @return The evaluation result.
     */
    @Transactional
    public GradingDto evaluate(SubmitSubmissionDto<TriggerSubmissionDto> submission) {
        // find task
        var task = this.taskRepository.findById(submission.taskId()).orElseThrow(() -> new EntityNotFoundException("Task " + submission.taskId() + " does not exist."));

        //Process submission input
        Locale locale = Locale.of(submission.language());
        Long taskId = submission.taskId();
        String generalFeedback = "";
        List<CriterionDto> criterionDtoList = new ArrayList<>();
        BigDecimal points = null;

        // evaluate input
        LOG.info("Evaluating input for task {} with mode {} and feedback-level {}", submission.taskId(), submission.mode(), submission.feedbackLevel());

        String mode = submission.mode().name();
        TriggerExecutionService triggerExecutionService = new TriggerExecutionService(this.triggerDataSourceService);
        ExecutionResult executionResult;
        List<Snapshot> taskSolutionExecution = new ArrayList<>();
        TriggerAnalyzer triggerAnalyzer;
        TriggerHeadEvaluation triggerHeadEvaluation;
        switch (mode){
            case "RUN":
                //execute trigger
                executionResult = triggerExecutionService.executeUserSubmission(task, submission, true);
                //analyze trigger execution
                triggerAnalyzer = new TriggerAnalyzer(submission, messageSource, taskSolutionExecution, task.getMaxPoints(), executionResult, null);
                if (executionResult.isSuccessful()) {
                    triggerAnalyzer.buildResultTables();
                }
                //generate feedback
                generalFeedback = triggerAnalyzer.getGeneralFeedback();
                criterionDtoList = triggerAnalyzer.getCriteria();
                break;
                /*
                // return syntax error if present
                if (!executionResult.isSyntaxError()) {
                    generalFeedback = messageSource.getMessage("noSyntaxError", null, locale);
                    criterionDtoList.add( new CriterionDto(
                        messageSource.getMessage("criterium.syntax", null, locale),
                        null,
                        true,
                        messageSource.getMessage("criterium.syntax.valid",  null, locale)
                    ));
                } else {
                    generalFeedback = messageSource.getMessage("syntaxError", null, locale);
                    generalFeedback = generalFeedback + "\n" + executionResult.getExecutionMessage();
                    criterionDtoList.add( new CriterionDto(
                        messageSource.getMessage("criterium.syntax", null, locale),
                        null,
                        false,
                        messageSource.getMessage("criterium.syntax.invalid",  null, locale)
                    ));
                }
                break;
                */
            case "DIAGNOSE":
                if (task.isBuffered()) {
                    //check if execution is already present
                    if (BufferedSnapshots.getInstance().containsTask(taskId, true)) {
                        taskSolutionExecution = BufferedSnapshots.getInstance().getSnapshotsByTaskIdAndMode(taskId, true);
                    } else {
                        executionResult = triggerExecutionService.executeTask(task, true);
                        taskSolutionExecution = executionResult.getExecutionResult();
                        BufferedSnapshots.getInstance().addSnapshots(taskSolutionExecution);
                    }
                } else {
                    executionResult = triggerExecutionService.executeTask(task, true);
                    taskSolutionExecution = executionResult.getExecutionResult();
                }
                //analyze trigger head
                triggerHeadEvaluation = new TriggerHeadEvaluation(submission, task, messageSource);
                triggerHeadEvaluation.analyze();
                //execute trigger
                executionResult = triggerExecutionService.executeUserSubmission(task, submission, true);
                //analyze trigger execution
                triggerAnalyzer = new TriggerAnalyzer(submission, messageSource, taskSolutionExecution, task.getMaxPoints(), executionResult, triggerHeadEvaluation);
                triggerAnalyzer.analyze();
                //generate feedback
                generalFeedback = triggerAnalyzer.getGeneralFeedback();
                criterionDtoList = triggerAnalyzer.getCriteria();
                points = triggerAnalyzer.getPoints();
                break;
            case "SUBMIT":
                if (task.isBuffered()) {
                    //check if execution is already present
                    if (BufferedSnapshots.getInstance().containsTask(taskId, false)) {
                        taskSolutionExecution = BufferedSnapshots.getInstance().getSnapshotsByTaskIdAndMode(taskId, false);
                    } else {
                        executionResult = triggerExecutionService.executeTask(task, false);
                        taskSolutionExecution = executionResult.getExecutionResult();
                        BufferedSnapshots.getInstance().addSnapshots(taskSolutionExecution);
                    }
                } else {
                    executionResult = triggerExecutionService.executeTask(task, false);
                    taskSolutionExecution = executionResult.getExecutionResult();
                }
                //analyze trigger head
                triggerHeadEvaluation = new TriggerHeadEvaluation(submission, task, messageSource);
                triggerHeadEvaluation.analyze();
                //execute trigger
                executionResult = triggerExecutionService.executeUserSubmission(task, submission, true);
                //analyze trigger execution
                triggerAnalyzer = new TriggerAnalyzer(submission, messageSource, taskSolutionExecution, task.getMaxPoints(), executionResult, triggerHeadEvaluation);
                triggerAnalyzer.analyze();
                //generate feedback
                generalFeedback = triggerAnalyzer.getGeneralFeedback();
                criterionDtoList = triggerAnalyzer.getCriteria();
                points = triggerAnalyzer.getPoints();
                break;
        }

        return new GradingDto(task.getMaxPoints(), points, generalFeedback, criterionDtoList);
    }
}
