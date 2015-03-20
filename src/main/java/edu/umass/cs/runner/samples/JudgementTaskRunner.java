package edu.umass.cs.runner.samples;

import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.system.backend.AbstractResponseManager;
import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.surveyman.samples.JudgementTask;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.IOException;

/**
 * Created by etosch on 3/5/15.
 */
public class JudgementTaskRunner {

    public static void main(String[] args)
            throws SurveyException, IOException, InterruptedException {
        Survey survey = JudgementTask.makeSurvey();
        Runner runner = Runner.init("LOCALHOST");
        Record record = AbstractResponseManager.getRecord(survey);
        runner.runAll(survey, record.classifier, false);
    }
}
