package edu.umass.cs.runner;

import edu.umass.cs.runner.system.Parameters;
import edu.umass.cs.runner.system.SurveyResponse;
import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.runner.system.backend.ITask;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

public class Record implements Serializable {

    final private static Logger LOGGER = Runner.LOGGER;
    final private static Gensym gensym = new Gensym(String.format("rec_%d", System.currentTimeMillis()));

    public String outputFileName;
    final public Survey survey;
    public AbstractLibrary library;
    final public Classifier classifier;
    final public boolean smoothing;
    final public double alpha;
    final public String rid = gensym.next();
    private List<SurveyResponse> validResponses;
    private List<SurveyResponse> botResponses;
    private Deque<ITask> tasks; // these should be hitids
    private String htmlFileName = "";
    public KnownBackendType backendType;
    public final double expectedCost;
    private final String RECORDDIR = AbstractLibrary.RECORDDIR + AbstractLibrary.fileSep + this.rid;

    public String serializeRecord()
            throws IOException
    {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String serializedFileName = RECORDDIR + AbstractLibrary.fileSep + timestamp;
        FileOutputStream fileOutputStream = new FileOutputStream(serializedFileName);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(this);
        objectOutputStream.close();
        fileOutputStream.close();
        LOGGER.info("Wrote record data to " + serializedFileName);
        return serializedFileName;
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
            boolean madeOutDir = (new File(AbstractLibrary.OUTDIR)).mkdir();
            LOGGER.debug(String.format("Made new ouput directory: %b", madeOutDir));
            boolean madeLogdDir = (new File("logs")).mkdir();
            LOGGER.debug(String.format("Made new logs directory: %b", madeLogdDir));
            File outfile = new File(String.format("%s%s%s_%s_%s.csv"
                    , AbstractLibrary.OUTDIR
                    , AbstractLibrary.fileSep
                    , survey.sourceName
                    , survey.sid
                    , AbstractLibrary.TIME));
            boolean madeOutFile = outfile.createNewFile();
            LOGGER.debug(String.format("Made new outputfile %s: %b", outfile, madeOutFile));
            File htmlFileName = new File(String.format("%s%slogs%s%s_%s_%s.html"
                    , (new File("")).getAbsolutePath()
                    , AbstractLibrary.fileSep
                    , AbstractLibrary.fileSep
                    , survey.sourceName
                    , survey.sid
                    , AbstractLibrary.TIME));
            boolean createdNewFile = false;
            if (! htmlFileName.exists())
                createdNewFile = htmlFileName.createNewFile();
            LOGGER.debug(String.format("Created new HTML file %s: %b", htmlFileName.getAbsolutePath(), createdNewFile));
            File recordDir = new File(this.RECORDDIR);
            boolean madeRecordDirs = false;
            if (! recordDir.exists())
                madeRecordDirs = recordDir.mkdirs();
            LOGGER.debug(String.format("Created new record directory %s: %b", recordDir.getAbsolutePath(),
                    madeRecordDirs));
            this.outputFileName = outfile.getCanonicalPath();
            this.htmlFileName = htmlFileName.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.survey = survey;
        this.library = someLib; //new MturkLibrary();
        this.validResponses = new ArrayList<SurveyResponse>();
        this.botResponses = new ArrayList<SurveyResponse>();
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
        try {
            this.serializeRecord();
        } catch (IOException io) {
            LOGGER.warn(String.format("Attempt to serialize record %s in constructor failed.,", this.rid));
            LOGGER.warn(io);
        }
    }

    public Record(
            final Survey survey,
            AbstractLibrary someLib,
            KnownBackendType backendType)
    {
        this(survey, someLib, Classifier.ALL, false, -0.0, backendType);
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
        } catch (Exception e) {
            System.out.println(StringUtils.join(this.library.props.propertyNames(), "\n"));
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
        if (this.tasks == null || this.tasks.isEmpty())
            return new ITask[0];
        return this.tasks.toArray(new ITask[tasks.size()]);
    }

    public synchronized List<SurveyResponse> getAllResponses()
    {
        List<SurveyResponse> allResponses = new ArrayList<SurveyResponse>();

        if (this.validResponses != null) {
            for (SurveyResponse SurveyResponse : validResponses)
                allResponses.add((SurveyResponse) SurveyResponse);
        }
        if (this.botResponses != null) {
            for (SurveyResponse SurveyResponse : botResponses)
                allResponses.add((SurveyResponse) SurveyResponse);
        }
        return allResponses;
    }

    public String jsonizeResponses()
            throws SurveyException
    {
        List<SurveyResponse> SurveyResponses = this.getAllResponses();
        QCMetrics.classifyResponses(this.survey, new ArrayList<SurveyResponse>(SurveyResponses), this.classifier, this.smoothing, this.alpha);
        List<String> strings = new ArrayList<String>();
        for (SurveyResponse sr : this.getAllResponses()) {
            strings.add(sr.makeStruct().jsonize());
        }
        return String.format("[ %s ]", StringUtils.join(strings, ", "));
    }

    public boolean needsWrite()
    {
        for (SurveyResponse SurveyResponse : this.getAllResponses())
            if (!SurveyResponse.isRecorded())
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
            SurveyResponse surveyResponse)
    {
        this.botResponses.add(surveyResponse);
    }

    public synchronized void addValidResponse(
            SurveyResponse surveyResponse)
    {
        this.validResponses.add(surveyResponse);
    }

    public synchronized void removeBotResponse(
            SurveyResponse surveyResponse)
    {
        if (this.botResponses.contains(surveyResponse)) {
            this.botResponses.remove(surveyResponse);
        }
    }

    public synchronized void removeValidResponse(
            SurveyResponse surveyResponse)
    {
        if (this.validResponses.contains(surveyResponse)) {
            this.botResponses.remove(surveyResponse);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Record) {
            Record that = (Record) o;

            int     thisNumResponses = this.getAllResponses().size(),
                    thatNumResponses = that.getAllResponses().size(),
                    thisNumTasks = this.getAllTasks().length,
                    thatNumTasks = that.getAllTasks().length;

            boolean outputFileEqual = this.outputFileName.equals(that.outputFileName),
                    surveyEqual = this.survey.equals(that.survey),
                    libraryEqual = this.library.equals(that.library),
                    classifierEqual = this.classifier.equals(that.classifier),
                    smoothingEqual = this.smoothing == that.smoothing,
                    alphaEqual = this.alpha == that.alpha,
                    ridEqual = this.rid.equals(that.rid),
                    numResponsesEqual = thisNumResponses == thatNumResponses,
                    numTasksEqual = thisNumTasks == thatNumTasks,
                    htmlFileEqual = this.htmlFileName.equals(that.htmlFileName),
                    backendTypeEqual = this.backendType.equals(that.backendType);
            if (!outputFileEqual) {
                LOGGER.debug(String.format("Record output filenames not equal (%s vs. %s)", this.outputFileName, that.outputFileName));
                return false;
            } else if (!surveyEqual) {
                LOGGER.debug(String.format("Surveys not equal (%s vs %s)", this.survey, that.survey));
                return false;
            } else if (!libraryEqual) {
                LOGGER.debug(String.format("Libraries not equal (%s vs. %s)", this.library, that.library));
                return false;
            } else if (!classifierEqual) {
                LOGGER.debug(String.format("Classifiers not equal: (%s vs. %s)", this.classifier, that.classifier));
                return false;
            } else if (!smoothingEqual) {
                LOGGER.debug(String.format("Smoothing not equal (%b vs. %b)", this.smoothing, that.smoothing));
                return false;
            } else if (!alphaEqual) {
                LOGGER.debug(String.format("Alpha not equal (%f vs. %f)", this.alpha, that.alpha));
                return false;
            } else if (!ridEqual){
                LOGGER.debug(String.format("Record ids not equal (%s vs. %s)", this.rid, that.rid));
                return false;
            } else if (!numResponsesEqual) {
                LOGGER.debug(String.format("Number of responses not equal (%d vs. %d)", thisNumResponses, thatNumResponses));
                return false;
            } else if (!numTasksEqual) {
                LOGGER.debug(String.format("Number of tasks not equal (%d vs. %d)", thisNumTasks, thatNumTasks));
                return false;
            } else if (!htmlFileEqual) {
                LOGGER.debug(String.format("HTML files not equal (%s vs. %s)", this.htmlFileName, that.htmlFileName));
                return false;
            } else if (!backendTypeEqual) {
                LOGGER.debug(String.format("Backend type not equal (%s vs. %s)", this.backendType.name(), that.backendType.name()));
                return false;
            } else return true;
        } else return false;
    }
}

