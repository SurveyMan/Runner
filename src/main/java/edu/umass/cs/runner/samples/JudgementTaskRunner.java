package edu.umass.cs.runner.samples;

import edu.umass.cs.runner.Runner;
import edu.umass.cs.surveyman.samples.JudgementTask;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

/**
 * Created by etosch on 3/5/15.
 */
public class JudgementTaskRunner {

    public static void main(String[] args)
            throws SurveyException
    {
        Survey survey = JudgementTask.makeSurvey();
        Runner.runAll();
    }
}
