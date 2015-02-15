package edu.umass.cs.runner;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.amazonaws.mturk.service.exception.AccessKeyException;
import com.amazonaws.mturk.service.exception.InsufficientFundsException;
import edu.umass.cs.runner.system.BoxedBool;
import edu.umass.cs.runner.system.backend.*;
import edu.umass.cs.runner.system.Parameters;
import edu.umass.cs.runner.system.backend.known.localhost.LocalLibrary;
import edu.umass.cs.runner.system.backend.known.localhost.LocalResponseManager;
import edu.umass.cs.runner.system.backend.known.localhost.LocalSurveyPoster;
import edu.umass.cs.runner.system.backend.known.localhost.Server;
import edu.umass.cs.runner.system.backend.known.localhost.server.WebServerException;
import edu.umass.cs.runner.system.backend.known.mturk.MturkLibrary;
import edu.umass.cs.runner.system.backend.known.mturk.MturkResponseManager;
import edu.umass.cs.runner.system.backend.known.mturk.MturkSurveyPoster;
import edu.umass.cs.runner.utils.ArgReader;
import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;

public class Runner {

    public static final Logger LOGGER = LogManager.getLogger(Runner.class.getName());
    private static long timeSinceLastNotice = System.currentTimeMillis();
    private static final String LOCALDIR = ".surveyman";
    private static final String CURRENT_DASHBOARD_DATA = String.format(
            "%s%scurrent_dashboard.json", LOCALDIR, AbstractLibrary.fileSep);
    public static KnownBackendType backendType;
    public static AbstractResponseManager responseManager;
    public static ISurveyPoster surveyPoster;
    public static AbstractLibrary library;
    public static final BoxedBool interrupt = new BoxedBool();
    public static final double getBasePay = 7.25;

    public static void makeLocalFolder()
    {
        File f = new File(".surveyman");
        if (!f.exists())
            assert f.mkdir();
            LOGGER.info(f.getAbsolutePath());
    }

    public static ArgumentParser makeArgParser()
    {
        // move more of the setup into this method
        ArgumentParser argumentParser = ArgumentParsers.newArgumentParser(Runner.class.getName(),true,"-").description("Posts surveys");
        argumentParser.addArgument("survey").required(true);
        for (Map.Entry<String, String> entry : ArgReader.getMandatoryAndDefault(Runner.class).entrySet()) {
            String arg = entry.getKey();
            Argument a = argumentParser.addArgument("--" + arg)
                    .required(true)
                    .help(ArgReader.getDescription(arg));
            String[] c = ArgReader.getChoices(arg);
            if (c.length>0)
                a.choices(c);

        }
        for (Map.Entry<String, String> entry : ArgReader.getOptionalAndDefault(Runner.class).entrySet()){
            String arg = entry.getKey();
            Argument a = argumentParser.addArgument("--" + arg)
                    .required(false)
                    .setDefault(entry.getValue())
                    .help(ArgReader.getDescription(arg));
            String[] c = ArgReader.getChoices(arg);
            if (c.length>0)
                a.choices(c);
        }
        return argumentParser;
    }

    public static void init(
            String bt,
            String properties,
            String config)
            throws IOException
    {
        // if it's an unrecognized backend type, it will fail earlier
        backendType = KnownBackendType.valueOf(bt);
        switch (backendType) {
            case LOCALHOST:
                responseManager = new LocalResponseManager();
                surveyPoster = new LocalSurveyPoster();
                library = new LocalLibrary(properties);
                break;
            case MTURK:
                library = new MturkLibrary(properties, config);
                responseManager = new MturkResponseManager((MturkLibrary) library);
                surveyPoster = new MturkSurveyPoster();
                surveyPoster.init(config);
                break;
        }
        makeLocalFolder();
    }

    public static void init(
            String bt)
            throws IOException
    {
        init(bt, "","");
    }

    public static void init(
            KnownBackendType bt)
            throws IOException
    {
        init(bt.name());
    }

    public static int recordAllTasksForSurvey(
            Survey survey)
            throws IOException,
            SurveyException
    {

        Record record = AbstractResponseManager.getRecord(survey);
        String hiturl = "", msg;
        int responsesAdded = 0;

        for (ITask hit : record.getAllTasks()) {
            hiturl = surveyPoster.makeTaskURL(responseManager, hit);
            responsesAdded = responseManager.addResponses(survey, hit);
        }

        msg = String.format("Polling for responses for Tasks at %s (%d total; %d valid)"
                , hiturl
                , record.validResponses.size()+record.botResponses.size()
                , record.botResponses.size());

        if (System.currentTimeMillis() - timeSinceLastNotice > 90000) {
            System.out.println(msg);
            LOGGER.info(msg);
            timeSinceLastNotice = System.currentTimeMillis();
        }

        return responsesAdded;
    }

    public static Thread makeResponseGetter(
            final Survey survey)
    {
        // grab responses for each incomplete survey in the responsemanager
        final KnownBackendType backendType = Runner.backendType;
        return new Thread(){
            @Override
            public void run(){
                System.out.println(String.format("Checking for responses in %s", backendType));
                do {
                    try {
                        recordAllTasksForSurvey(survey);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SurveyException e) {
                        e.printStackTrace();
                    }
                } while(!interrupt.getInterrupt());
                // if we're out of the loop, expire and process the remaining HITs
                try {
                    Record record = AbstractResponseManager.getRecord(survey);
                    ITask[] tasks = record.getAllTasks();
                    System.out.println("\n\tCleaning up...\n");
                    for (ITask task : tasks){
                        boolean expiredAndAdded = false;
                        while (! expiredAndAdded) {
                            try {
                                responseManager.makeTaskUnavailable(task);
                                responseManager.addResponses(survey, task);
                                expiredAndAdded = true;
                            } catch (Exception e) {
                                System.err.println("something in the response getter thread threw an error.");
                                e.printStackTrace();
                            }
                        }
                    }
                    AbstractResponseManager.removeRecord(record);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SurveyException e) {
                        e.printStackTrace();
                    }
                }
        };
    }

    public static boolean stillLive(
            Survey survey)
            throws IOException,
            SurveyException,
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException
    {
        Record record = AbstractResponseManager.getRecord(survey);
        if (record==null) return false;
        boolean done = record.validResponses.size() >= Integer.parseInt(record.library.props.getProperty(Parameters.NUM_PARTICIPANTS));
        return ! done;
    }

    public static void writeResponses(
            Survey survey,
            Record record)
    {
        List<AbstractSurveyResponse> responseList = new ArrayList<AbstractSurveyResponse>(record.validResponses);
        for (AbstractSurveyResponse sr : responseList) {
            if (!sr.isRecorded()) {
                BufferedWriter bw = null;
                LOGGER.info("Writing " + sr.getSrid() + "...");
                try {
                    LOGGER.info(record.outputFileName);
                    File f = new File(record.outputFileName);
                    bw = new BufferedWriter(new FileWriter(f, true));
                    if (! f.exists() || f.length()==0)
                        bw.write(ResponseWriter.outputHeaders(survey));
                    String txt = ResponseWriter.outputSurveyResponse(survey, sr);
                    bw.write(txt);
                    sr.setRecorded(true);
                    bw.close();
                } catch (IOException ex) {
                    LOGGER.info(ex.getMessage());
                    LOGGER.warn(ex);
                } finally {
                    try {
                        if (bw != null) bw.close();
                    } catch (IOException ex) {
                        LOGGER.warn(ex);
                    }
                }
            }
        }
    }

    public static Thread makeWriter(
            final Survey survey)
    {
        //writes hits that correspond to current jobs in memory to their files
        return new Thread(){
            @Override
            public void run(){
                Record record = null;
                do {
                    try {
                        record = AbstractResponseManager.getRecord(survey);
                        writeResponses(survey, record);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SurveyException e) {
                        e.printStackTrace();
                    } catch (NullPointerException npe) {
                        LOGGER.warn(npe);
                    }
                } while (!interrupt.getInterrupt());
                    // clean up
                System.out.print("Writing straggling data...");
                if (record!=null)
                    writeResponses(survey, record);
                System.out.println("done.");
            }
        };
    }

    public static void dashboardDump(Namespace ns){
        try {
            FileWriter out = new FileWriter(CURRENT_DASHBOARD_DATA);
            List<String> strings = new ArrayList<String>();
            for (Map.Entry<String, Object> entry : ns.getAttrs().entrySet())
                strings.add(String.format("\"%s\" : \"%s\"", entry.getKey(), entry.getValue()));
            out.write("{ " + StringUtils.join(strings, ", ") + " }");
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            LOGGER.fatal(e);
            System.exit(1);
        } catch (IOException io) {
            LOGGER.fatal(io);
            System.exit(1);
        }
    }

    public static void run(
            final Record record)
            throws InterruptedException,
            ClassNotFoundException,
            InstantiationException,
            IllegalAccessException,
            IOException,
            AccessKeyException
    {
        try {
            Survey survey = record.survey;
            do {
                if (!interrupt.getInterrupt()) {
                    surveyPoster.postSurvey(responseManager, record);
                }
            } while (stillLive(survey));
            Object foo = new Object(){};
            interrupt.setInterrupt(true, String.format("Target goal met in %s.%s"
                    , foo.getClass().getEnclosingClass().getName()
                    , foo.getClass().getEnclosingMethod().getName()));

        } catch (SurveyException e) {

            String msg = "Fatal error: " + e.getMessage() + "\nExiting...";
            System.err.println(msg);
            LOGGER.fatal(e);
            interrupt.setInterrupt(true,"Error detected in edu.umass.cs.runner.Runner.run",e.getStackTrace()[1]);

        } finally {

            synchronized(interrupt) {
                surveyPoster.stopSurvey(responseManager, record, interrupt);
            }

        }
    }

    public static Thread makeRunner(
            final Record record)
    {
        return new Thread(){
            @Override
            public void run() {
                try {
                    Runner.run(record);
                } catch (InsufficientFundsException ife) {
                    System.out.println("Insufficient funds in your Mechanical Turk account. Would you like to:\n" +
                        "[1] Add more money to your account and retry\n" +
                        "[2] Quit\n");
                    int i = 0;
                    while(i!=1 && i!=2){
                        System.out.println("Type number corresponding to preference: ");
                        i = new Scanner(System.in).nextInt();
                        if (i==2)
                            System.exit(1);
                    }
                } catch (AccessKeyException aws) {
                    System.out.println(String.format("There is a problem with your access keys: %s; Exiting...", aws.getMessage()));
                    System.exit(0);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public static void runAll(
            String s,
            String sep)
            throws InvocationTargetException,
            IllegalAccessException,
            NoSuchMethodException,
            IOException,
            InterruptedException
    {
        try {
            CSVParser csvParser = new CSVParser(new CSVLexer(s, sep));
            Survey survey = csvParser.parse();
            // create and store the record
            final Record record = new Record(survey, library, backendType);
            AbstractResponseManager.putRecord(survey, record);
            // now we're ready to go
            Thread writer = makeWriter(survey);
            Thread responder = makeResponseGetter(survey);
            Thread runner = makeRunner(record);
            runner.start();
            writer.start();
            responder.start();
            StringBuilder msg = new StringBuilder(String.format("Target number of valid responses: %s\nTo take the survey, navigate to:"
                    , record.library.props.get(Parameters.NUM_PARTICIPANTS)));
            while (record.getAllTasks().length==0) {}
            for (ITask task : record.getAllTasks())
                msg.append("\n\t" + surveyPoster.makeTaskURL(responseManager, task));
            LOGGER.info(msg.toString());
            System.out.println(msg.toString());
            runner.join();
            responder.join();
            writer.join();
        } catch (SurveyException se) {
            System.err.println("Fatal error: " + se.getMessage() + "\nExiting...");
        }
    }

    public static void runDashboard(Namespace ns){
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("edu.umass.cs.runner.dashboard.Dashboard"));
        IFn run = Clojure.var("edu.umass.cs.runner.dashboard.Dashboard", "run");
        run.invoke(ns);
    }

    public static void main(
            String[] args)
            throws IOException,
            InterruptedException,
            NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException,
            ParseException,
            WebServerException,
            InstantiationException,
            ClassNotFoundException
    {

        ArgumentParser argumentParser = makeArgParser();
        Namespace ns;
        try {
            ns = argumentParser.parseArgs(args);

            init(ns.getString("backend"), ns.getString("properties"), ns.getString("config"));

            if (backendType.equals(KnownBackendType.LOCALHOST))
                Server.startServe();

            dashboardDump(ns);

            runDashboard(ns);

            runAll(ns.getString("survey"), ns.getString("separator"));

            if (backendType.equals(KnownBackendType.LOCALHOST))
                Server.endServe();

            String msg = String.format("Shutting down. Execute this program with args %s to repeat.", Arrays.toString(args));
            LOGGER.info(msg);

        } catch (ArgumentParserException e) {
            System.err.println("FAILURE: "+e.getMessage());
            LOGGER.fatal(e);
            argumentParser.printHelp();
        }
    }
}
