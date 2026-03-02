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
    private final Map<CriterionDto, Integer> criterionDtos;
    private final MessageSource messageSource;
    private final String mode;
    private final Locale locale;
    private boolean headPenalty;

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
            this.criterionDtos = new HashMap<>();
            this.mode = submission.mode().name();
            this.headPenalty = false;
        }
    }

    private Map<String, String> checkTrigger(String triggerDefinition) {
        // Regular expression pattern to parse the trigger head
        String triggerPattern = "(?i)CREATE(?: OR REPLACE)? TRIGGER\\s+(\\w+)\\s+(BEFORE|AFTER|INSTEAD OF)\\s+((?:INSERT|UPDATE(?: OF [\\w, ]+)?|DELETE)(?: OR (?:INSERT|UPDATE(?: OF [\\w, ]+)?|DELETE))*)\\s+ON\\s+(\\w+)";

        // Compile the pattern
        Pattern pattern = Pattern.compile(triggerPattern);
        Matcher matcher = pattern.matcher(triggerDefinition.strip().toUpperCase());
        Map<String, String> result = new HashMap<>();

        if (matcher.find()) {
            // Extract components, all parts are present
            result.put(messageSource.getMessage("criteria.triggerName", null, this.locale), matcher.group(1));
            result.put(messageSource.getMessage("criteria.triggerTiming", null, this.locale), matcher.group(2));
            result.put(messageSource.getMessage("criteria.triggerEvent", null, this.locale), matcher.group(3));
            result.put(messageSource.getMessage("criteria.triggerTableName", null, this.locale), matcher.group(4));
        } else {
            // Some trigger parts are missing
            // Check for trigger name
            headPenalty = true;
            Pattern namePattern = Pattern.compile("(?:^|\\s)CREATE(?: OR REPLACE)? TRIGGER (\\w+)");
            Matcher nameMatcher = namePattern.matcher(triggerDefinition);
            if (nameMatcher.find()) {
                result.put(messageSource.getMessage("criteria.triggerName", null, this.locale), nameMatcher.group(1));
            } else {
                result.put(messageSource.getMessage("criteria.triggerName", null, this.locale), "NOT OK");
            }

            // Check for timing (BEFORE, AFTER, INSTEAD OF)
            Pattern timingPattern = Pattern.compile("(?:^|\\s)(BEFORE|AFTER|INSTEAD OF) ");
            Matcher timingMatcher = timingPattern.matcher(triggerDefinition);
            if (timingMatcher.find()) {
                result.put(messageSource.getMessage("criteria.triggerTiming", null, this.locale), timingMatcher.group(1));
            } else {
                result.put(messageSource.getMessage("criteria.triggerTiming", null, this.locale), "NOT OK");
            }

            // Check for event (INSERT, UPDATE, DELETE, INSERT OR UPDATE)
            Pattern eventPattern = Pattern.compile("(?:^|\\s)(INSERT OR UPDATE|INSERT|UPDATE|DELETE) ");
            Matcher eventMatcher = eventPattern.matcher(triggerDefinition);
            if (eventMatcher.find()) {
                result.put(messageSource.getMessage("criteria.triggerEvent", null, this.locale), eventMatcher.group(1));
            } else {
                result.put(messageSource.getMessage("criteria.triggerEvent", null, this.locale), "NOT OK");
            }

            // Check for table name (ON <table>)
            Pattern tablePattern = Pattern.compile("(ON|UPDATE OF) (\\w+)");
            Matcher tableMatcher = tablePattern.matcher(triggerDefinition);
            if (tableMatcher.find()) {
                result.put(messageSource.getMessage("criteria.triggerTableName", null, this.locale), tableMatcher.group(1));
            } else {
                result.put(messageSource.getMessage("criteria.triggerTableName", null, this.locale), "NOT OK");
            }

        }
        return result;
    }

    private void addCriteria(BigDecimal points, boolean passed, String criteria, int showAtFeedbackLevel) {
        String name = messageSource.getMessage("criteria.triggerHead", null, this.locale);
        criterionDtos.put(new CriterionDto(name, points, passed, criteria), showAtFeedbackLevel);
    }

    public void analyze() {
        Map<String, String> parsedSolution = checkTrigger(solutionTrigger.toUpperCase().strip());
        Map<String, String> parsedUser = checkTrigger(userTrigger.toUpperCase().strip());

        for (Map.Entry<String, String> entry : parsedUser.entrySet()) {
            //ignore trigger name
            if(!entry.getKey().equals(messageSource.getMessage("criteria.triggerName", null, this.locale))) {
                if (!entry.getValue().equals(parsedSolution.get(entry.getKey()))) {
                    if (entry.getKey().equals(messageSource.getMessage("criteria.triggerTiming", null, this.locale)) && task.isTimingIndependent()) {
                        //special handling timing independent tasks
                        if (!((entry.getValue().equals("BEFORE") || entry.getValue().equals("AFTER")) &&
                            (parsedSolution.get(entry.getKey()).equals("BEFORE") || parsedSolution.get(entry.getKey()).equals("AFTER")))) {
                            //event of submission or solution is neither BEFORE nor AFTER
                            addCriteria(null, false, entry.getKey(), 3);
                            this.headPenalty = true;
                        }
                    } else {
                        addCriteria(null, false, entry.getKey(), 3);
                        this.headPenalty = true;
                    }
                }
            }

        }
    }

    public boolean isHeadPenalty() {
        return headPenalty;
    }

    public Map<CriterionDto, Integer> getCriterionDtos() {
        if (headPenalty) {
            addCriteria(task.getMaxPoints().multiply(task.getWrongHeadPenalty()), false, messageSource.getMessage("criteria.triggerHeadNotOk", null, this.locale), 2);
        } else {
            addCriteria(null, true, messageSource.getMessage("criteria.triggerHeadOk", null, this.locale), 2);
        }
        return criterionDtos;
    }

    public TriggerTask getTask() {
        return task;
    }
}
