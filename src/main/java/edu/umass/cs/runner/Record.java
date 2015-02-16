package edu.umass.cs.runner;

import edu.umass.cs.runner.system.Parameters;
import edu.umass.cs.runner.system.SurveyResponse;
import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.runner.system.backend.ITask;
import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Record {

    final private static Logger LOGGER = Logger.getLogger(Record.class);
    final private static Gensym gensym = new Gensym("rec");

    public String outputFileName;
    final public Survey survey;
    public AbstractLibrary library;
    final public Classifier classifier;
    final public boolean smoothing;
    final public double alpha;
    final public String rid = gensym.next();
    public List<AbstractSurveyResponse> validResponses;
    public List<AbstractSurveyResponse> botResponses;
    private Deque<ITask> tasks; // these should be hitids
    private String htmlFileName = "";
    public KnownBackendType backendType;
    public final double expectedCost;


    public Record(
            final Survey survey,
            AbstractLibrary someLib,
            Classifier classifier,
            boolean smoothing,
            double alpha,
            KnownBackendType backendType)
    {
        try {
            (new File(AbstractLibrary.OUTDIR)).mkdir();
            (new File("logs")).mkdir();
            File outfile = new File(String.format("%s%s%s_%s_%s.csv"
                    , AbstractLibrary.OUTDIR
                    , AbstractLibrary.fileSep
                    , survey.sourceName
                    , survey.sid
                    , AbstractLibrary.TIME));
            outfile.createNewFile();
            File htmlFileName = new File(String.format("%s%slogs%s%s_%s_%s.html"
                    , (new File("")).getAbsolutePath()
                    , AbstractLibrary.fileSep
                    , AbstractLibrary.fileSep
                    , survey.sourceName
                    , survey.sid
                    , AbstractLibrary.TIME));
            if (! htmlFileName.exists())
                htmlFileName.createNewFile();
            this.outputFileName = outfile.getCanonicalPath();
            this.htmlFileName = htmlFileName.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.survey = survey;
        this.library = someLib; //new MturkLibrary();
        this.validResponses = new ArrayList<AbstractSurveyResponse>();
        this.botResponses = new ArrayList<AbstractSurveyResponse>();
        this.tasks = new ArrayDeque<ITask>();
        this.backendType = backendType;
        this.classifier = classifier;
        this.smoothing = smoothing;
        this.alpha = alpha;
        this.expectedCost = computeExpectedCost();
        LOGGER.info(String.format("New record with id (%s) created for survey %s (%s)."
                , rid
                , survey.sourceName
                , survey.sid
        ));
    }

    public Record(
            final Survey survey,
            AbstractLibrary someLib,
            KnownBackendType backendType)
    {
        this(survey, someLib, null, false, -0.0, backendType);
    }

    private double computeExpectedCost()
    {
        try {
            double averagePathLength = QCMetrics.averagePathLength(this.survey);
            int n = Integer.parseInt((String) this.library.props.get(Parameters.NUM_PARTICIPANTS));
            return averagePathLength *
                   (AbstractLibrary.timePerQuestionInSeconds / 3600) *
                   AbstractLibrary.FEDMINWAGE *
                   n;
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
        return 0.0;
    }

    public static String getHtmlFileName(Survey survey) throws IOException {
        return new File(String.format("%s%slogs%s%s_%s_%s.html"
                , (new File("")).getAbsolutePath()
                , AbstractLibrary.fileSep
                , AbstractLibrary.fileSep
                , survey.sourceName
                , survey.sid
                , AbstractLibrary.TIME)).getCanonicalPath();
    }

    public String getHtmlFileName(){
        return this.htmlFileName;
    }

    public void addNewTask(ITask task) {
        tasks.push(task);
    }

    public ITask[] getAllTasks()
    {
        if (this.tasks.isEmpty())
            return new ITask[0];
        return this.tasks.toArray(new ITask[tasks.size()]);
    }

    public synchronized List<SurveyResponse> getAllResponses()
    {
        List<SurveyResponse> allResponses = new ArrayList<SurveyResponse>();

        for (AbstractSurveyResponse abstractSurveyResponse : validResponses)
            allResponses.add((SurveyResponse) abstractSurveyResponse);
        for (AbstractSurveyResponse abstractSurveyResponse : botResponses)
            allResponses.add((SurveyResponse) abstractSurveyResponse);
        return allResponses;
    }

    public String jsonizeResponses()
            throws SurveyException
    {
        List<SurveyResponse> abstractSurveyResponses = this.getAllResponses();
        QCMetrics.classifyResponses(this.survey, new ArrayList<AbstractSurveyResponse>(abstractSurveyResponses), this.classifier, this.smoothing, this.alpha);
        List<String> strings = new ArrayList<String>();
        for (SurveyResponse sr : this.getAllResponses()) {
            strings.add(sr.makeStruct().jsonize());
        }
        return String.format("[ %s ]", StringUtils.join(strings, ", "));
    }

    public boolean needsWrite()
    {
        for (AbstractSurveyResponse abstractSurveyResponse : this.getAllResponses())
            if (!abstractSurveyResponse.isRecorded())
                return true;
        return false;
    }

}

