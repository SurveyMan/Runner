package edu.umass.cs.runner.system.backend;


import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.BoxedBool;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public interface ISurveyPoster {

    ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException;
    boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt);
    String makeTaskURL(AbstractResponseManager responseManager, ITask task);
    void init(String config);
}
