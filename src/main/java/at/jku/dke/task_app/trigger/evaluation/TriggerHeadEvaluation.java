package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.trigger.data.entities.TriggerTask;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriggerHeadEvaluation {

    private final TriggerTask task;
    private final SubmitSubmissionDto<TriggerSubmissionDto> submission;
    private final String solutionTrigger;
    private final String userTrigger;
    private final List<CriterionDto> criterionDtos;
    private final MessageSource messageSource;
    private final int feedbackLevel;
    private final String mode;
    private final Locale locale;
    private boolean parsable;

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
            this.criterionDtos = new ArrayList<>();
            this.mode = submission.mode().name();
            this.parsable = true;
            this.feedbackLevel = mode.equals("SUBMIT") ? 0 : submission.feedbackLevel();
        }
    }

    private List<String> parseTrigger(String triggerDefinition) {
        // Regular expression pattern to parse the trigger head
        String triggerPattern = "(?i)CREATE(?: OR REPLACE)? TRIGGER\\s+(\\w+)\\s+(BEFORE|AFTER|INSTEAD OF)\\s+(INSERT OR UPDATE|INSERT|UPDATE|DELETE)\\s+ON\\s+(\\w+)";

        // Compile the pattern
        Pattern pattern = Pattern.compile(triggerPattern);
        Matcher matcher = pattern.matcher(triggerDefinition.strip());
        List<String> result = new ArrayList<>();

        if (matcher.find()) {
            // Extract components
            result.add(matcher.group(1));
            result.add(matcher.group(2));
            result.add(matcher.group(3));
            result.add(matcher.group(4));
        }
        return result;
    }

    private void addCriteria(String criteria, int showAtFeedbackLevel) {
        this.parsable = false;
        if (showAtFeedbackLevel <= this.feedbackLevel) {
            this.criterionDtos.add(new CriterionDto(
                this.messageSource.getMessage("criteria.triggerHead", null, this.locale),
                null,
                false,
                criteria));
        }
    }

    public boolean hasParsableHead() {
        List<String> parsedSolution = parseTrigger(solutionTrigger);
        List<String> parsedUser = parseTrigger(userTrigger);
        Boolean result = true;
        if (parsedSolution.size() != parsedUser.size()) {
            addCriteria(messageSource.getMessage("criteria.triggerHeadNotParsable", null, this.locale), 2);
            result = false;
        } else {
            for (int i = 0; i < parsedSolution.size(); i++) {
                if (!parsedSolution.get(i).equals(parsedUser.get(i))) {
                    result = false;
                    switch (i) {
                        case 0:
                            addCriteria(messageSource.getMessage("criteria.triggerName", null, this.locale), 3);
                            break;
                        case 1:
                            addCriteria(messageSource.getMessage("criteria.triggerTiming", null, this.locale), 3);
                            break;
                        case 2:
                            addCriteria(messageSource.getMessage("criteria.triggerEvent", null, this.locale), 3);
                            break;
                        case 3:
                            addCriteria(messageSource.getMessage("criteria.triggerTableName", null, this.locale), 3);
                            break;
                    }
                }
            }
        }
        return result;
    }

    public List<CriterionDto> getCriteria() {
        return this.criterionDtos;
    }

    public String getGeneralFeedback() {
        if (!parsable) {
            return this.messageSource.getMessage("criteria.triggerHeadNotParsable", null, this.locale);
        } else
            return this.messageSource.getMessage("criteria.triggerHeadParsable", null, this.locale);
    }

    public BigDecimal getPoints() {
        if (!parsable) {
            return task.getMaxPoints().add(task.getMaxPoints().multiply(task.getWrongHeadPenalty()));
        } else {
            return task.getMaxPoints();
        }
    }
}
