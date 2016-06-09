package edu.umass.cs.runner.samples;

import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.runner.system.backend.known.localhost.Server;
import edu.umass.cs.runner.system.backend.known.localhost.server.WebServerException;
import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.samples.JudgementTask;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by etosch on 3/5/15.
 */
public class JudgementTaskRunner {

    public static void main(String[] args)
            throws SurveyException, InterruptedException, NoSuchMethodException, IOException, IllegalAccessException,
                   InvocationTargetException, WebServerException
    {
        Runner.init(KnownBackendType.LOCALHOST);
        Server.startServe();
        Survey survey = JudgementTask.makeSurvey();
        Runner.runAll(survey, SurveyMan.resolveClassifier(survey, "lpo", 2, 0.05, false), false, 0.05, true);
        Server.endServe();
    }
}
