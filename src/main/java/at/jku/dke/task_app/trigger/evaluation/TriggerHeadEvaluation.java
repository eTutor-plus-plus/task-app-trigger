package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriggerHeadEvaluation {

    private final TriggerTask task;
    private final SubmitSubmissionDto<TriggerSubmissionDto> submission;
    private final String solutionTrigger;
    private final String userTrigger;
    private Map<Integer, String> generalFeedBack;
    private final Map<CriterionDto, Integer> criterionDtos;
    private final MessageSource messageSource;
    private final String mode;
    private final Locale locale;
    private boolean correct;

    public TriggerHeadEvaluation(SubmitSubmissionDto<TriggerSubmissionDto> submission, TriggerTask task, MessageSource messageSource) {
        if (submission.feedbackLevel() < 0 || submission.feedbackLevel() > 3) {
            throw new IllegalArgumentException("feedbackLevel must be between 0 and 3");
        } else {
            this.submission = submission;
            this.task = task;
            this.messageSource = messageSource;
            this.locale = Locale.of(submission.language());
            this.solutionTrigger = task.getSolution();
            this.userTrigger = submission.submission().input();
            this.generalFeedBack = new HashMap<>();
            this.criterionDtos = new HashMap<>();
            this.mode = submission.mode().name();
            this.correct = true;
        }
    }

    private Map<Integer, String> checkTrigger(String triggerDefinition) {
        // Regular expression pattern to parse the trigger head
        String triggerPattern = "(?i)CREATE(?: OR REPLACE)? TRIGGER\\s+(\\w+)\\s+(BEFORE|AFTER|INSTEAD OF)\\s+(INSERT OR UPDATE|INSERT|UPDATE|DELETE)\\s+ON\\s+(\\w+)";

        // Compile the pattern
        Pattern pattern = Pattern.compile(triggerPattern);
        Matcher matcher = pattern.matcher(triggerDefinition.strip().toUpperCase());
        Map<Integer, String> result = new HashMap<>();

        if (matcher.find()) {
            // Extract components
            result.put(1, matcher.group(1));
            result.put(2, matcher.group(2));
            result.put(3,matcher.group(3));
            result.put(4, matcher.group(4));
        }
        return result;
    }

    private void addCriteria(String criteria, boolean passed, int showAtFeedbackLevel) {
        this.criterionDtos.put(new CriterionDto(
            this.messageSource.getMessage("criteria.triggerHead", null, this.locale),
            null,
            passed,
            criteria), showAtFeedbackLevel);
    }

    public boolean analyze() {
        Map<Integer, String> parsedSolution = checkTrigger(solutionTrigger);
        Map<Integer, String> parsedUser = checkTrigger(userTrigger);
        Boolean result = true;
        if (parsedSolution.size() != parsedUser.size()) {
            addCriteria(messageSource.getMessage("criteria.triggerHeadNotOk", null, this.locale), false, 2);
            result = false;
        }
        for (int i = 1; i <= 4; i++) {
            if (!parsedSolution.get(i).equals(parsedUser.get(i))) {
                result = false;

                switch (i) {
                    case 1:
                        addCriteria(messageSource.getMessage("criteria.triggerName", null, this.locale), false, 3);
                        break;
                    case 2:
                        addCriteria(messageSource.getMessage("criteria.triggerTiming", null, this.locale), false, 3);
                        break;
                    case 3:
                        addCriteria(messageSource.getMessage("criteria.triggerEvent", null, this.locale), false, 3);
                        break;
                    case 4:
                        addCriteria(messageSource.getMessage("criteria.triggerTableName", null, this.locale), false, 3);
                        break;
                }
            }
        }
        if (result) {
            addCriteria(messageSource.getMessage("criteria.triggerHeadOk", null, this.locale), true,2);
        } else {
            addCriteria(messageSource.getMessage("criteria.triggerHeadNotOk", null, this.locale), false,2);
        }
        this.correct = result;
        return result;
    }

    public boolean isCorrect() {
        return correct;
    }

    public Map<CriterionDto, Integer> getCriteria() {
        return this.criterionDtos;
    }

    public Map<Integer, String> getGeneralFeedback() {
        if (!correct) {
            generalFeedBack.put(1, this.messageSource.getMessage("incorrect", null, this.locale));
            generalFeedBack.put(2, this.messageSource.getMessage("criteria.triggerHeadNotOk", null, this.locale));
        } else {
            generalFeedBack.put(2, this.messageSource.getMessage("criteria.triggerHeadOk", null, this.locale));
        }
        return this.generalFeedBack;
    }

    public BigDecimal getPoints() {
        if (!correct) {
            return task.getMaxPoints().add(task.getMaxPoints().multiply(task.getWrongHeadPenalty()));
        } else {
            return task.getMaxPoints();
        }
    }

    public TriggerTask getTask() {
        return task;
    }
}
