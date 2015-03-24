package edu.umass.cs.runner.system.backend;


import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.BoxedBool;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public interface ISurveyPoster {

    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException;
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt);
    public String makeTaskURL(AbstractResponseManager responseManager, ITask task);
    public void init(String config);
    public boolean stillLive(Survey survey, Schedule schedule);

}
