package edu.umass.cs.runner.system;

import edu.umass.cs.runner.Runner;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionResponse implements IQuestionResponse {

    private Question q;
    private List<OptTuple> opts = new ArrayList<OptTuple>();
    private int indexSeen;
    /** otherValues is a map of the key value pairs that are not necessary for quality control,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    public Map<String, String> otherValues = new HashMap<String, String>();


    public QuestionResponse()
    {

    }

    public QuestionResponse(
            Survey s,
            String quid,
            int qpos)
            throws SurveyException
    {
        this.q = s.getQuestionById(quid);
        this.indexSeen = qpos;
    }

    public QuestionResponse(
            Question q)
    {
        this.q = q;
    }

    public String quid()
    {
        return q.id;
    }

    public void add(
            String quid,
            OptTuple tupe,
            Map<String, String> otherValues)
    {
        this.otherValues = otherValues;
        if (this.q == null) {
            this.q = Question.makeQuestion("", -1, -1);
            this.q.id = quid;
        }
        this.opts.add(tupe);
        this.indexSeen = -1;
    }

    public void add(
            JSONObject response,
            Survey s,
            Map<String,String> otherValues)
            throws SurveyException, JSONException {

        boolean custom = Question.customQuestion(response.getString("quid"));
        this.otherValues.putAll(otherValues);

        if (custom){
            this.q = Question.makeQuestion("CUSTOM", -1, -1);
            this.indexSeen = response.getInt("qpos");
            this.opts.add(new OptTuple(new StringDatum(response.getString("oid"), -1, -1, -1), -1));
        } else {
            this.q = s.getQuestionById(response.getString("quid"));
            this.indexSeen = response.getInt("qpos");
            // do something
            if (q.freetext) {
//                Runner.LOGGER.warn("In freetext -- do something?");
            }
            else {
                SurveyDatum c = s.getQuestionById(q.id).getOptById(response.getString("oid"));
                int optloc = response.getInt("opos");
                this.opts.add(new OptTuple(c, optloc));
            }
        }
    }

    @Override
    public boolean equals(
            Object that) {
        return that instanceof QuestionResponse && this.q.equals(((QuestionResponse) that).q)
                && this.opts.equals(((QuestionResponse) that).opts);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (OptTuple o : opts){
            s.append(o.c.toString());
        }
        return String.format(" (%s) %s : [ %s ]", q.id, q.toString(), s.toString());
    }

    public Question getQuestion() {
        return this.q;
    }

    public List<OptTuple> getOpts() {
        return this.opts;
    }

    public int getIndexSeen() {
        return this.indexSeen;
    }

    public SurveyDatum getAnswer() throws SurveyException {
        if (this.getQuestion().exclusive)
            return this.getOpts().get(0).c;
        else throw new RuntimeException("Cannot call getAnswer() on non-exclusive questions. Try getAnswers() instead.");

    }

    public List<SurveyDatum> getAnswers() throws SurveyException {
        if (this.getQuestion().exclusive)
            throw new RuntimeException("Cannot call getAnswers() on exclusive questions. Try getAnswer() instead.");
        List<SurveyDatum> answers = new ArrayList<SurveyDatum>();
        for (OptTuple optTuple : this.getOpts())
            answers.add(optTuple.c);
        return answers;
    }

    public int compareTo(Object o) {
        if (o instanceof IQuestionResponse) {
            IQuestionResponse that = (IQuestionResponse) o;
            return this.getQuestion().compareTo(that.getQuestion());
        } else throw new RuntimeException(String.format("Cannot compare classes %s and %s",
                this.getClass().getName(), o.getClass().getName()));    }
}
