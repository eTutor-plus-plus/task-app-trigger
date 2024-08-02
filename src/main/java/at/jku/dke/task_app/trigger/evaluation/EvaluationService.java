package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.trigger.data.repositories.TriggerTaskRepository;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.BufferedSnapshots;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.Snapshot;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.Trigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
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
    private final TriggerDataSource triggerDataSource;

    /**
     * Creates a new instance of class {@link EvaluationService}.
     *
     * @param taskRepository    The task repository.
     * @param messageSource     The message source.
     * @param triggerDataSource The datsource.
     */
    public EvaluationService(TriggerTaskRepository taskRepository, MessageSource messageSource, TriggerDataSource triggerDataSource) {
        this.taskRepository = taskRepository;
        this.messageSource = messageSource;
        this.triggerDataSource = triggerDataSource;
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
        String generalFeedback = this.messageSource.getMessage("correct", null, locale);
        List<CriterionDto> criterionDtoList = new ArrayList<>();
        BigDecimal points = task.getMaxPoints();
        Boolean successfulExecution = true;

        // evaluate input
        LOG.info("Evaluating input for task {} with mode {} and feedback-level {}", submission.taskId(), submission.mode(), submission.feedbackLevel());

        String mode = submission.mode().name();
        int feedbackLevel = submission.feedbackLevel();
        //TODO: evalueate umbauen auf switch --> bessere code übersicht
        //TODO: mode und level bereits verarbeitet übergeben
        switch (mode){
            case "RUN":
                //TODO: only syntax check
                //METHOD create schema and create trigger
                //METHOD return syntax error if present, etc
                break;
            case "DIAGNOSE":
                //TODO: run and add criteria, feedback, with levels
                //METHOD parse trigger head
                //METHOD create schema and trigger and run statements
                //METHOD return feedback and criteria
                break;
            case "SUBMIT":
                //TODO: only run no feedback
                //METHOD create schema and trigger and run statements
                break;
        }

        //Trigger head
        TriggerHeadEvaluation triggerHeadEvaluation = new TriggerHeadEvaluation(submission, task, messageSource);
        if (!triggerHeadEvaluation.hasParsableHead()) {
            //User Trigger is not parsable, return feedback
            successfulExecution = false;
            generalFeedback = triggerHeadEvaluation.getGeneralFeedback();
            criterionDtoList.addAll(triggerHeadEvaluation.getCriteria());
            points = triggerHeadEvaluation.getPoints();
        } else {
            //Add criteria Trigger head is correct
            criterionDtoList.add(new CriterionDto(
                this.messageSource.getMessage("criteria.triggerHead", null, locale),
                null,
                true,
                this.messageSource.getMessage("criteria.triggerHeadOk", null, locale)
            ));

            //Execute task solution
            List<Snapshot> taskSolutionExecution;
            TriggerTaskExecution triggerTaskExecution = new TriggerTaskExecution(this.triggerDataSource);
            if (task.isBuffered()) {
                if (BufferedSnapshots.getInstance().containsTask(taskId)) {
                    taskSolutionExecution = BufferedSnapshots.getInstance().getSnapshotsByTaskId(taskId);
                } else {
                    taskSolutionExecution = triggerTaskExecution.executeTask(task);
                    BufferedSnapshots.getInstance().addSnapshots(taskSolutionExecution);
                }
            } else {
                taskSolutionExecution = triggerTaskExecution.executeTask(task);
            }

            TriggerBodyEvaluation triggerBodyEvaluation = new TriggerBodyEvaluation(submission, task, this.messageSource);
            try (Connection userConnection = this.triggerDataSource.getUserSchemaConnection()) {
                if (triggerBodyEvaluation.createUserTrigger(userConnection)) {
                    if (!submission.mode().name().equals("RUN")) {
                        //Handle run differently --> only check syntax, no comparison
                        if (triggerBodyEvaluation.executeUserTrigger(userConnection)) {
                            successfulExecution = triggerBodyEvaluation.compareResults(taskSolutionExecution);
                        }
                    }
                }
                triggerBodyEvaluation.resetConnection(userConnection);
                this.triggerDataSource.clearUserSchemaConnection(userConnection);
                generalFeedback = triggerBodyEvaluation.getGeneralFeedback();
                criterionDtoList.addAll(triggerBodyEvaluation.getCriteria());
                points = triggerBodyEvaluation.getPoints();
            } catch (SQLException ex) {
                LOG.error("Failed to connect to database", ex);
            }
        }

        if(submission.mode().name().equals("RUN")) {
            //If mode is run return only if syntax is correct
            if (successfulExecution) {
                generalFeedback = messageSource.getMessage("possiblyCorrect", null, locale);
                criterionDtoList.clear();
                criterionDtoList.add( new CriterionDto(
                    messageSource.getMessage("criterium.syntax", null, locale),
                    null,
                    true,
                    messageSource.getMessage("criterium.syntax.valid",  null, locale)
                ));
            } else {
                generalFeedback = messageSource.getMessage("incorrect", null, locale);
                criterionDtoList.clear();
                criterionDtoList.add( new CriterionDto(
                    messageSource.getMessage("criterium.syntax", null, locale),
                    null,
                    false,
                    messageSource.getMessage("criterium.syntax.invalid",  null, locale)
                ));
            }
        } else if(submission.mode().name().equals("SUBMIT") || submission.feedbackLevel() < 1) {
            //No feedback --> return nothing
            generalFeedback = "";
            criterionDtoList.clear();
            points = null;
        }
        else if (submission.feedbackLevel() == 1) {
            //Little Feedback --> only see if submission was executed successfully or not
            if (successfulExecution) {
                generalFeedback = messageSource.getMessage("correct", null, locale);
            } else {
                generalFeedback = messageSource.getMessage("incorrect", null, locale);
                criterionDtoList.clear();
            }
        }
        return new GradingDto(task.getMaxPoints(), points, generalFeedback, criterionDtoList);
    }
}
