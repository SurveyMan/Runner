package edu.umass.cs.runner;

import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.runner.system.backend.ITask;
import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.utils.Gensym;
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
    final public String rid = gensym.next();
    public List<AbstractSurveyResponse> validResponses;
    public List<AbstractSurveyResponse> botResponses;
    private Deque<ITask> tasks; // these should be hitids
    private String htmlFileName = "";
    public KnownBackendType backendType;


    public Record(final Survey survey, AbstractLibrary someLib, KnownBackendType backendType)  {
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
        LOGGER.info(String.format("New record with id (%s) created for survey %s (%s)."
                , rid
                , survey.sourceName
                , survey.sid
        ));
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

    public ITask[] getAllTasks() {
        if (this.tasks.isEmpty())
            return new ITask[0];
        return this.tasks.toArray(new ITask[tasks.size()]);
    }

}

