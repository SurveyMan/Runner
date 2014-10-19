import com.amazonaws.mturk.service.exception.AccessKeyException;
import edu.umass.cs.runner.AbstractResponseManager;
import edu.umass.cs.runner.BackendType;
import edu.umass.cs.runner.system.mturk.MturkLibrary;
import edu.umass.cs.runner.system.mturk.MturkResponseManager;
import edu.umass.cs.runner.system.mturk.MturkSurveyPoster;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.ISurveyPoster;
import edu.umass.cs.runner.system.ITask;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

@RunWith(JUnit4.class)
public class MTurkTest extends TestLog {

    static class SurveyTasksTuple {
        public Survey s;
        public ITask hits;
        public SurveyTasksTuple(Survey s, ITask hits) {
            this.s = s; this.hits = hits;
        }
    }

    public MTurkTest() throws Exception {
        super.init(this.getClass());
    }

    private SurveyTasksTuple sendSurvey(int i)
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException, InstantiationException {
        CSVParser parser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
        Survey survey = parser.parse();
        MturkLibrary lib = new MturkLibrary();
        Record record = new Record(survey, lib, BackendType.MTURK);
        AbstractResponseManager responseManager = new MturkResponseManager(lib);
        ISurveyPoster surveyPoster = new MturkSurveyPoster();
        record.library.props.setProperty("hitlifetime", "3000");
        record.library.props.setProperty("sandbox", "true");
        MturkResponseManager.putRecord(survey, record);
        ITask hits = surveyPoster.postSurvey(responseManager, record);
        assert (hits!=null);
        return new SurveyTasksTuple(survey, hits);
    }

    @Test
    public void testRenew()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
            , ParseException, InstantiationException {
        try {
            SurveyTasksTuple stuff  = sendSurvey(1);
            Survey survey = stuff.s;
            ITask hit = stuff.hits;
            AbstractResponseManager responseManager = new MturkResponseManager(new MturkLibrary());
            responseManager.makeTaskUnavailable(hit);
            if (! ((MturkResponseManager) responseManager).renewIfExpired(hit.getTaskId(), survey))
                throw new RuntimeException("Didn't renew.");
            responseManager.makeTaskAvailable(hit.getTaskId(), responseManager.getRecord(survey));
            responseManager.makeTaskUnavailable(hit);
          }catch(AccessKeyException aws) {
            LOGGER.warn(aws);
            System.out.println(aws);
            return;
          } catch (SurveyException se){
            if (outcome[1])
                throw se;
            else return;
        }
    }

}
