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
    private Locale locale;
    private String mode;
    private boolean bodyPentalty;

    public TriggerAnalyzer(SubmitSubmissionDto<TriggerSubmissionDto> submission, MessageSource messageSource, List<Snapshot> taskSolutionExecution, BigDecimal maxPoints, ExecutionResult executionResult) {
        this.messageSource = messageSource;
        this.triggerHeadEvaluation = null;
        this.taskSolutionExecution = taskSolutionExecution;
        this.executionResult = executionResult;

        this.feedBackLevel = submission.feedbackLevel();
        this.generalFeedBack = new HashMap<>();
        generalFeedBack.put(0, "");
        this.criterionDtos = new HashMap<>();
        this.points = maxPoints;
        this.locale = Locale.of(submission.language());
        this.mode = submission.mode().name();
        this.bodyPentalty = false;
    }

    public TriggerAnalyzer(SubmitSubmissionDto<TriggerSubmissionDto> submission, MessageSource messageSource, List<Snapshot> taskSubmissionExecution, BigDecimal maxPoints, ExecutionResult executionResult, TriggerHeadEvaluation triggerHeadEvaluation) {
        this(submission, messageSource, taskSubmissionExecution, maxPoints, executionResult);
        this.triggerHeadEvaluation = triggerHeadEvaluation;
    }

    public void analyze() {
        //TODO: feedback und criteria überarbeiten
        this.criterionDtos.putAll(triggerHeadEvaluation.getCriteria());
        this.generalFeedBack.putAll(triggerHeadEvaluation.getGeneralFeedback());
        if (!triggerHeadEvaluation.isCorrect()) {
            //head incorrect further analyzation not needed, 0 points
            this.points = triggerHeadEvaluation.getPoints();
        } else if (executionResult.isSuccsfull() ) {
            //compare result sets
            List<Snapshot> taskSubmissionExecution = executionResult.getExecutionResult();
            if(taskSubmissionExecution.size() == taskSubmissionExecution.size()) {
                Map<String, Snapshot> taskSubmissionExecutionMap = new HashMap<>();
                for (Snapshot snapshot : taskSubmissionExecution) {
                    taskSubmissionExecutionMap.put(snapshot.getExecutionStatement(), snapshot);
                }
                for (Snapshot solution : taskSolutionExecution) {
                    String executionStatement = solution.getExecutionStatement();
                    Snapshot submission = taskSubmissionExecutionMap.get(executionStatement);
                    if (submission == null) {
                        //this shouldn't happen
                        LOG.error("Unforeseen comparison error, snapshot set are not equal!");
                        return;
                    } else {
                        if (!compareSnapshots(solution, submission)) {
                            generalFeedBack.put(1, this.messageSource.getMessage("incorrect", null, locale) + "\n " + executionResult.getExecutionMessage());
                        }
                    }
                }
                if(bodyPentalty) {
                    generalFeedBack.put(2, this.messageSource.getMessage("wrongTriggerBody", null, locale));
                } else {
                    generalFeedBack.put(1, this.messageSource.getMessage("possiblyCorrect", null, locale));
                    generalFeedBack.remove(2);
                }
            } else {
                //this shouldn't happen
                LOG.error("Unforeseen comparison error, snapshot set are not equal!");
                return;
            }
        } else {
            generalFeedBack.put(1, this.messageSource.getMessage("syntaxError", null, locale) + "\n " + executionResult.getExecutionMessage());
            generalFeedBack.remove(2);
            if (triggerHeadEvaluation.isCorrect()) {
                generalFeedBack.put(3, this.messageSource.getMessage("headOkSyntaxError", null, locale) + "\n " + executionResult.getExecutionMessage());
                //subtract points for wrong body
                this.points = points.add(points.multiply(triggerHeadEvaluation.getTask().getWrongBodyPenalty()));
            }
        }
    }

    private boolean compareSnapshots(Snapshot solution, Snapshot submission) {
        // Assume the comparison is correct until proven otherwise
        boolean correct = true;

        for (Map.Entry<String, List<List<String>>> entry : solution.getTablesSnapshot().entrySet()) {
            String table = entry.getKey();
            List<List<String>> solutionSet = entry.getValue();
            List<List<String>> submissionSet = submission.getTablesSnapshot().get(table);
            String executionStatement = submission.getExecutionStatement();

            if (submissionSet != null) {
                // Compare the result sets
                if (!compareResultSet(solutionSet, submissionSet)) {
                    if(!this.bodyPentalty) {
                        this.points = points.add(points.multiply(triggerHeadEvaluation.getTask().getWrongBodyPenalty()));
                        this.bodyPentalty = true;
                    }
                    correct = false;
                    String name = this.messageSource.getMessage("criterium.executedStatement", null, locale);
                    String criteria = this.messageSource.getMessage("criterium.executedStatement.wrongComparison", new Object[]{table, executionStatement}, locale);
                    addCriteria(name, new BigDecimal(0), false, criteria, 3);
                }
            } else {
                // Log error if the submission set is missing
                LOG.error("Unforeseen comparison error, snapshot sets are not equal!");
                return false;
            }
        }
        return correct;
    }

    private boolean compareResultSet(List<List<String>> solutionSet, List<List<String>> submissionSet) {
        if (solutionSet.equals(submissionSet)) {
            return true;
        } else {
            return false;
        }
    }

    private void addCriteria(String name, BigDecimal points, boolean passed, String criteria, int showAtFeedbackLevel) {
        criterionDtos.put(new CriterionDto(name, points, passed, criteria), showAtFeedbackLevel);
    }

    public String getGeneralFeedback() {
        int effectiveFeedbackLevel;
        if (mode.equals("SUBMIT")) {
            effectiveFeedbackLevel = 0;
        } else {
            effectiveFeedbackLevel = feedBackLevel;
        }
        String result = null;
        while(result == null) {
            result = generalFeedBack.get(effectiveFeedbackLevel);
            effectiveFeedbackLevel--;
        }
        return result;
    }

    public List<CriterionDto> getCriteria() {
        int effectiveFeedbackLevel;
        if (mode.equals("SUBMIT")) {
            effectiveFeedbackLevel = 0;
        } else {
            effectiveFeedbackLevel = feedBackLevel;
        }

        List<Map.Entry<CriterionDto, Integer>> filteredList = criterionDtos.entrySet()
            .stream()
            .filter(entry -> entry.getValue() <= effectiveFeedbackLevel)
            .sorted(Map.Entry.comparingByValue()) // Sort by value (v)
            .toList();

        List<CriterionDto> resultList = filteredList.stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return resultList;
    }

    public BigDecimal getPoints() {
        if(this.feedBackLevel > 0) {
            return this.points;
        } else {
            return null;
        }
    }
}
