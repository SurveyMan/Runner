package edu.umass.cs.runner.system.backend.known.localhost;

import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.system.backend.*;
import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.BoxedBool;
import edu.umass.cs.runner.system.generators.HTML;
import edu.umass.cs.runner.system.backend.known.localhost.generators.LocalHTML;
import edu.umass.cs.runner.system.backend.known.localhost.server.WebServerException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.File;
import java.io.IOException;

public class LocalSurveyPoster implements ISurveyPoster {

    @Override
    public void init(String configURL) {

    }

    @Override
    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException {

        ITask task = null;

        if (r.getAllTasks().length>0)
            return task;

        try {
            task = new LocalTask(r);
            HTML.spitHTMLToFile(HTML.getHTMLString(r, new LocalHTML()), r.survey);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return task;
    }

    @Override
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt) {
       try {
           boolean success = Server.endSurvey();
           interrupt.setInterrupt(true, "Call to stop survey.", new Exception(){}.getStackTrace()[1]);
           return success;
       } catch (WebServerException se) {
           return false;
       }
    }

    @Override
    public String makeTaskURL(AbstractResponseManager am, ITask task) {
        Record r = task.getRecord();
        if (Server.serving)
            return String.format("http://localhost:%d/logs/%s",
                    Server.frontPort,
                    new File(r.getHtmlFileName()).getName());
        else return "";
    }
}
