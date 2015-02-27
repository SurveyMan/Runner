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

import java.io.*;
import java.util.*;

public class Record implements Serializable {

    final private static Logger LOGGER = Logger.getLogger(Record.class);
    final private static Gensym gensym = new Gensym(String.format("rec_%d", System.currentTimeMillis()));

    public String outputFileName;
    final public Survey survey;
    public AbstractLibrary library;
    final public Classifier classifier;
    final public boolean smoothing;
    final public double alpha;
    final public String rid = gensym.next();
    private List<AbstractSurveyResponse> validResponses;
    private List<AbstractSurveyResponse> botResponses;
    private Deque<ITask> tasks; // these should be hitids
    private String htmlFileName = "";
    public KnownBackendType backendType;
    public final double expectedCost;
    private final String RECORDDIR = AbstractLibrary.RECORDDIR + AbstractLibrary.fileSep + this.rid;

    public void serializeRecord()
            throws IOException
    {
        String timestamp = String.valueOf(System.currentTimeMillis());
        FileOutputStream fileOutputStream = new FileOutputStream(RECORDDIR + AbstractLibrary.fileSep + timestamp);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(this);
        objectOutputStream.close();
        fileOutputStream.close();
        LOGGER.info("Wrote record data to "+RECORDDIR+AbstractLibrary.fileSep+timestamp);
    }

    public static Record deserializeRecord(
            String serializedRecordFilename)
            throws IOException, ClassNotFoundException {
        Record record;
        FileInputStream fileInputStream = new FileInputStream(serializedRecordFilename);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        record = (Record) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();
        return record;
    }

    public static Record deserializeLatestRecord(
            String recordDirectory)
            throws IOException, ClassNotFoundException
    {
        File dir = new File(recordDirectory);
        assert dir.isDirectory() : String.format("File %s is not a directory", recordDirectory);
        long maxTimestamp = Long.MIN_VALUE;
        for (File file : dir.listFiles()) {
            long thisTimestamp = Long.parseLong(file.getName());
            if (thisTimestamp > maxTimestamp)
                maxTimestamp = thisTimestamp;
        }
        return deserializeRecord(recordDirectory + AbstractLibrary.fileSep + String.valueOf(maxTimestamp));
    }

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
            File recordDir = new File(this.RECORDDIR);
            if (! recordDir.exists())
                recordDir.mkdir();
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

    public void addNewTask(
            ITask task)
    {
        tasks.push(task);
        try {
            this.serializeRecord();
        } catch (IOException io) {
            LOGGER.warn("Attempted to serialize record:\n"+io);
        }
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

    public synchronized int getNumValidResponses() {
        return this.validResponses.size();
    }

    public synchronized int getNumBotResponses() {
        return this.botResponses.size();
    }

    public synchronized void addBotResponse(
            AbstractSurveyResponse surveyResponse)
    {
        this.botResponses.add(surveyResponse);
    }

    public synchronized void addValidResponse(
            AbstractSurveyResponse surveyResponse)
    {
        this.validResponses.add(surveyResponse);
    }

    public synchronized void removeBotResponse(
            AbstractSurveyResponse surveyResponse)
    {
        if (this.botResponses.contains(surveyResponse)) {
            this.botResponses.remove(surveyResponse);
        }
    }

    public synchronized void removeValidResponse(
            AbstractSurveyResponse surveyResponse)
    {
        if (this.validResponses.contains(surveyResponse)) {
            this.botResponses.remove(surveyResponse);
        }
    }
}

