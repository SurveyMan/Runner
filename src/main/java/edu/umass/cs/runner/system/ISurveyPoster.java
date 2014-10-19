package edu.umass.cs.runner.system;


import edu.umass.cs.runner.AbstractResponseManager;
import edu.umass.cs.runner.Record;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public interface ISurveyPoster {

    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException;
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt);
    public String makeTaskURL(AbstractResponseManager responseManager, ITask task);
    public void init(String config);
}
