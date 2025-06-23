package at.jku.dke.task_app.trigger.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.trigger.dto.TriggerSubmissionDto;
import at.jku.dke.task_app.trigger.evaluation.Snapshot.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class TriggerAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);

    private MessageSource messageSource;
    private TriggerHeadEvaluation triggerHeadEvaluation;
    private List<Snapshot> taskSolutionExecution;
    private ExecutionResult executionResult;

    private int feedBackLevel;
    private Map<Integer, String> generalFeedBack;
    private Map<CriterionDto, Integer> criterionDtos;
    private BigDecimal points;
    private final BigDecimal maxPoints;
    private Locale locale;
    private String mode;
    private boolean bodyPenalty;
    private boolean syntaxPenalty;
    private boolean headerPenalty;

    public TriggerAnalyzer(SubmitSubmissionDto<TriggerSubmissionDto> submission, MessageSource messageSource, List<Snapshot> taskSolutionExecution, BigDecimal maxPoints, ExecutionResult executionResult) {
        this.messageSource = messageSource;
        this.triggerHeadEvaluation = null;
        this.taskSolutionExecution = taskSolutionExecution;
        this.executionResult = executionResult;

        this.feedBackLevel = submission.feedbackLevel();
        this.generalFeedBack = new HashMap<>();
        this.criterionDtos = new HashMap<>();
        this.points = maxPoints;
        this.maxPoints = maxPoints;
        this.locale = Locale.of(submission.language());
        this.mode = submission.mode().name();
        //assume the comparison is correct until proven otherwise
        this.bodyPenalty = false;
        this.syntaxPenalty = false;
    }

    public TriggerAnalyzer(SubmitSubmissionDto<TriggerSubmissionDto> submission, MessageSource messageSource, List<Snapshot> taskSubmissionExecution, BigDecimal maxPoints, ExecutionResult executionResult, TriggerHeadEvaluation triggerHeadEvaluation) {
        this(submission, messageSource, taskSubmissionExecution, maxPoints, executionResult);
        this.triggerHeadEvaluation = triggerHeadEvaluation;
    }

    public void analyze() {
        this.criterionDtos.putAll(triggerHeadEvaluation.getCriterionDtos());
        this.headerPenalty = triggerHeadEvaluation.isHeadPenalty();
        this.syntaxPenalty = executionResult.isSyntaxError();

        //if either the syntax is wrong or the head is incorrect, always fail the comparison
        if (syntaxPenalty || headerPenalty) {
            bodyPenalty = true;
        }

        if (executionResult.isSuccessful() ) {
            buildResultTables();
            //compare result sets
            List<Snapshot> taskSubmissionExecution = executionResult.getExecutionResult();
            if(taskSolutionExecution.size() == taskSubmissionExecution.size()) {
                Map<String, Snapshot> taskSubmissionExecutionMap = new HashMap<>();
                for (Snapshot snapshot : taskSubmissionExecution) {
                    taskSubmissionExecutionMap.put(snapshot.getExecutionStatement(), snapshot);
                }
                for (Snapshot solution : taskSolutionExecution) {
                    String executionStatement = solution.getExecutionStatement();
                    Snapshot submission = taskSubmissionExecutionMap.get(executionStatement);
                    if (submission == null) {
                        //this shouldn't happen
                        LOG.error("Unforeseen comparison error, snapshot is null!");
                        String error = messageSource.getMessage("unforeseenComparisonError", null, locale);
                        generalFeedBack.clear();
                        generalFeedBack.put(0, error);
                        addCriteria(error, null, false, error, 3);
                        return;
                    } else {
                        compareSnapshots(solution, submission);
                    }
                }
            } else {
                //this shouldn't happen
                LOG.error("Unforeseen comparison error, snapshot sets are not equal!");
                String error = messageSource.getMessage("unforeseenComparisonError", null, locale);
                generalFeedBack.clear();
                generalFeedBack.put(0, error);
                addCriteria(error, null, false, error, 3);
            }
        } else {
            // execution fail because of runtime error of any kind
            this.bodyPenalty = true;
        }
    }

    private void buildResultTables() {
        for(String table : executionResult.getModifiedStates().keySet()) {
            StringBuilder sb = new StringBuilder("<div style=\"display: inline-block; vertical-align: top; margin-right: 20px;\"> ");
            sb.append(messageSource.getMessage("tableInitialState", new Object[]{table}, locale));
            sb.append(createTable(executionResult.getTableHeaders().get(table), executionResult.getInitialStates().get(table)));
            sb.append(" </div> <div style=\"display: inline-block; vertical-align: top;\"> ");
            sb.append(messageSource.getMessage("tableModifiedState", new Object[]{table}, locale));
            sb.append(createTable(executionResult.getTableHeaders().get(table), executionResult.getModifiedStates().get(table)));
            sb.append(" </div>");

            addCriteria(table, null, true, sb.toString(), 1);
        }
    }

    private void compareSnapshots(Snapshot solution, Snapshot submission) {
        for (Map.Entry<String, List<List<String>>> entry : solution.getTablesSnapshot().entrySet()) {
            String table = entry.getKey();
            List<List<String>> solutionSet = entry.getValue();
            List<List<String>> submissionSet = submission.getTablesSnapshot().get(table);
            String executionStatement = submission.getExecutionStatement();

            if (submissionSet != null) {
                // Compare the result sets
                if (!solutionSet.equals(submissionSet)) {
                    this.bodyPenalty = true;
                    String name = this.messageSource.getMessage("criterium.executedStatement", null, locale);
                    String criteria = this.messageSource.getMessage("criterium.executedStatement.wrongComparison", new Object[]{table, executionStatement}, locale);
                    addCriteria(name, null, false, criteria, 3);
                }
            } else {
                // Log error if the submission set is missing
                LOG.error("Unforeseen comparison error, snapshot sets are not equal!");
                String error = messageSource.getMessage("unforeseenComparisonError", null, locale);
                generalFeedBack.clear();
                generalFeedBack.put(0, error);
                addCriteria(error, null, false, error, 3);
                return;
            }
        }
    }

    private String createTable(List<String> columnHeaders, List<List<String>> tuples) {
        StringBuilder builder = new StringBuilder("<table border=\"1\"><thead><tr>");
        columnHeaders.forEach(c -> builder.append("<th>").append(c).append("</th>"));
        builder.append("</tr></head><tbody>");
        tuples.forEach(t -> {
            builder.append("<tr>");
            t.forEach(d -> builder.append("<td>").append(d).append("</td>"));
            builder.append("</tr>");
        });
        return builder.append("</tbody></table>").toString();
    }

    private void addCriteria(String name, BigDecimal points, boolean passed, String criteria, int showAtFeedbackLevel) {
        criterionDtos.put(new CriterionDto(name, points, passed, criteria), showAtFeedbackLevel);
    }

    public String getGeneralFeedback() {
        //no feedback for the lowest level
        this.generalFeedBack.put(0, "");

        //execution was not correct in any way
        if (syntaxPenalty || bodyPenalty || headerPenalty) {
            generalFeedBack.put(1, this.messageSource.getMessage("incorrect", null, this.locale));

            //the actual comparison was not correct
            if(bodyPenalty) {
                generalFeedBack.put(2, this.messageSource.getMessage("wrongTriggerBody", null, locale));
            }

            //the trigger head was no correct
            if (headerPenalty) {
                generalFeedBack.put(2, this.messageSource.getMessage("criteria.triggerHeadNotOk", null, this.locale));
            }

            //the syntax was not correct
            if (syntaxPenalty) {
                generalFeedBack.put(2, this.messageSource.getMessage("syntaxError", null, locale) + "\n" + executionResult.getExecutionMessage());
                //the syntax was not correct, but head generally was --> more detailed result for higher feedback
                if (!headerPenalty) {
                    generalFeedBack.put(3, this.messageSource.getMessage("headOkSyntaxError", null, locale) + "\n" + executionResult.getExecutionMessage());
                }
            }
        } else {
            //execution comparison was successful, finally distinguish between submission modes
            if (mode.equals("DIAGNOSE")) {
                generalFeedBack.put(1, this.messageSource.getMessage("possiblyCorrect", null, locale));
            } else {
                generalFeedBack.put(1, this.messageSource.getMessage("correct", null, locale));
            }
        }

        int effectiveFeedbackLevel = feedBackLevel;
        String result = null;
        while(result == null) {
            result = generalFeedBack.get(effectiveFeedbackLevel);
            effectiveFeedbackLevel--;
        }
        return result;
    }

    public List<CriterionDto> getCriteria() {
        if (syntaxPenalty) {
            String name = this.messageSource.getMessage("criterium.syntax", null, locale);
            String criteria = this.messageSource.getMessage("criterium.syntax.invalid", null, locale);
            addCriteria(name, null, false, criteria, 1);
        } else {
            String name = this.messageSource.getMessage("criterium.syntax", null, locale);
            String criteria = this.messageSource.getMessage("criterium.syntax.valid", null, locale);
            addCriteria(name, null, true, criteria, 1);
        }
        if (bodyPenalty) {
            String name = this.messageSource.getMessage("criteria.triggerBody", null, locale);
            String criteria = this.messageSource.getMessage("incorrect", null, locale);
            addCriteria(name, maxPoints.multiply(triggerHeadEvaluation.getTask().getWrongBodyPenalty()), false, criteria, 2);
        }
        if (!executionResult.isSuccessful()) {
            String name = this.messageSource.getMessage("criteria.executionMessage", null, locale);
            String criteria = executionResult.getExecutionMessage();
            addCriteria(name, null, false, criteria, 2);
        }

        List<CriterionDto> resultList = criterionDtos.entrySet()
            .stream()
            .filter(entry -> entry.getValue() <= feedBackLevel)
            .sorted(
                Comparator.comparing((Map.Entry<CriterionDto, Integer> entry) -> entry.getKey().passed())
                    .reversed()
                    .thenComparing(entry -> entry.getKey().name())
            )
            .map(entry -> {
                CriterionDto original = entry.getKey();
                if (feedBackLevel <= 2) {
                    //only show deducted points if feedBackLevel is greater than 2
                    return new CriterionDto(original.name(), null, original.passed(), original.feedback());
                } else {
                    return original;
                }
            })
            .collect(Collectors.toList());

        return resultList;
    }

    public BigDecimal getPoints() {
        if(headerPenalty) {
            this.points = this.points.add(triggerHeadEvaluation.getTask().getMaxPoints().multiply(triggerHeadEvaluation.getTask().getWrongHeadPenalty()));
            if (this.points.compareTo(BigDecimal.ZERO) < 0)
                //minimal points can be zero
                this.points = BigDecimal.ZERO;
        }
        if(bodyPenalty) {
            this.points = this.points.add(triggerHeadEvaluation.getTask().getMaxPoints().multiply(triggerHeadEvaluation.getTask().getWrongBodyPenalty()));
            if (this.points.compareTo(BigDecimal.ZERO) < 0)
                //minimal points can be zero
                this.points = BigDecimal.ZERO;
        }
        if(this.feedBackLevel > 0) {
            return this.points;
        } else {
            return null;
        }
    }
}
