package edu.umass.cs.runner.system.backend;

import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.utils.Slurpie;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractLibrary implements Serializable {

//    public static Logger LOGGER = Runner.LOGGER;
    public Properties props;
    public static final String fileSep = File.separator;

    // local configuration information
    protected static final String LOCALDIR = ".surveyman";
    public static final String CURRENT_DASHBOARD_DATA = String.format(
            "%s%scurrent_dashboard.json", LOCALDIR, AbstractLibrary.fileSep);
    public static final String OUTDIR = "output";
    public static final String PARAMS = LOCALDIR + fileSep + "params.properties";
    public static final String RECORDDIR =  LOCALDIR + fileSep + "records";

    // resources
    public static final String HTMLSKELETON = "HTMLSkeleton.html";
    public static final String JSSKELETON = LOCALDIR + fileSep + "custom.js";
    public static final String QUOTS = "quots";
    public static final String XMLSKELETON = "XMLSkeleton.xml";
    public static final String CUSTOMCSS = LOCALDIR + fileSep + "custom.css";

    // state/session/job information
    public static final String UNFINISHED_JOB_FILE = AbstractLibrary.LOCALDIR + AbstractLibrary.fileSep + "unfinished";
    public static final String BONUS_DATA = AbstractLibrary.LOCALDIR + AbstractLibrary.fileSep + "bonuses";
    public static final String TIME = String.valueOf(System.currentTimeMillis());
    public static final String STATEDATADIR = String.format("%1$s%2$s.data", LOCALDIR, fileSep);

    public static final double FEDMINWAGE = 7.25;
    public static double timePerQuestionInSeconds = 10;


    static {
        try {
            if (! new File(LOCALDIR).exists())
                new File(LOCALDIR).mkdir();
            if (! new File(OUTDIR).exists())
                new File(OUTDIR).mkdir();
            if (! new File(STATEDATADIR).exists())
                new File(STATEDATADIR).mkdir();
            if (! new File(UNFINISHED_JOB_FILE).exists())
                new File(UNFINISHED_JOB_FILE).createNewFile();
            if (! new File(BONUS_DATA).exists())
                new File(BONUS_DATA).createNewFile();
        } catch (IOException ex) {
//            Runner.LOGGER.fatal(ex);
        }
    }

    public abstract String getActionForm();
    public abstract void init();
    public abstract List<String> getBackendHeaders();
    @Override
    public abstract boolean equals(Object o);

    public void updateProperties(String filename) throws IOException {
        String foo = Slurpie.slurp(filename);
        props.load(new StringReader(foo));
    }


    public static void dashboardDump(
            Namespace ns)
    {
        try {
            FileWriter out = new FileWriter(AbstractLibrary.CURRENT_DASHBOARD_DATA);
            List<String> strings = new ArrayList<String>();
            for (Map.Entry<String, Object> entry : ns.getAttrs().entrySet())
                strings.add(String.format("\"%s\" : \"%s\"", entry.getKey(), entry.getValue()));
            out.write("{ " + StringUtils.join(strings, ", ") + " }");
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
//            Runner.LOGGER.fatal(e);
            System.exit(1);
        } catch (IOException io) {
//            Runner.LOGGER.fatal(io);
            System.exit(1);
        }
    }

    public static void get_params(){
        PrintWriter printWriter = new PrintWriter(System.out);
        printWriter.print(String.format(
                "I see this is the first time you have run edu.umass.cs.runner.Runner in directory %s. In order to " +
                        "continue, we will need some data.\n" +
                        "",
                new File(".").getAbsolutePath()));
    }

}