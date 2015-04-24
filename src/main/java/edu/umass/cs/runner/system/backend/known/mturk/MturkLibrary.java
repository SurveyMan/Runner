package edu.umass.cs.runner.system.backend.known.mturk;

import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.system.Parameters;
import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.survey.Survey;
import org.apache.commons.lang3.StringUtils;
import sun.swing.StringUIClientPropertyKey;

import java.io.*;
import java.security.acl.LastOwnerException;
import java.util.*;

public class MturkLibrary extends AbstractLibrary {

    public String CONFIG = LOCALDIR + fileSep + "mturk_config";

    private static final String MTURK_SANDBOX_URL = "https://mechanicalturk.sandbox.amazonaws.com?Service=AWSMechanicalTurkRequester";
    private static final String MTURK_PROD_URL = "https://mechanicalturk.amazonaws.com?Service=AWSMechanicalTurkRequester";
    private static final String MTURK_SANDBOX_EXTERNAL_HIT = "https://workersandbox.mturk.com/mturk/externalSubmit";
    private static final String MTURK_PROD_EXTERNAL_HIT = "https://www.mturk.com/mturk/externalSubmit";

    public String MTURK_URL;
    public String EXTERNAL_HIT;

    private List<String> backendHeaders = new ArrayList<String>();

    public String getActionForm() {
        return EXTERNAL_HIT;
    }
    // editable stuff gets copied

    public MturkLibrary(Properties properties, Survey survey) {
        init();
        this.props = properties;
        Runner.LOGGER.info("Updated properties to " + properties.toString());
        this.props.setProperty("reward", Double.toString(Runner.basePay));
    }

    public MturkLibrary(String propertiesURL, String configURL){
        if (configURL!=null)
            this.CONFIG = configURL;
        if (propertiesURL!=null) {
            super.props = new Properties();
            try {
                super.props.load(new FileReader(propertiesURL));
            } catch (IOException e) {
                System.err.println(String.format("%s\nCould not load properties from %s. Proceeding to load from default..."
                        , e.getMessage()
                        , propertiesURL));
            }
        }
        init();
    }

    public MturkLibrary(){
        init();
    }

    public void init() {

        // add the backend headers
        backendHeaders.add("submitTime");
        backendHeaders.add("acceptTime");

        try {
            File paramsFile = new File(AbstractLibrary.PARAMS);
            if (paramsFile.exists() && props==null){
                props = new Properties();
                Runner.LOGGER.info("Loading properties from " + AbstractLibrary.PARAMS);
                props.load(new FileReader(AbstractLibrary.PARAMS));
            } else {
                Runner.LOGGER.warn(String.format("%s exists: %b\n\tprops is null: %b",
                        AbstractLibrary.PARAMS, paramsFile.exists(), props==null));
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (props == null)
                props = new Properties();
        }

        Runner.LOGGER.info(props.toString());

        boolean sandbox = ! this.props.containsKey(Parameters.SANDBOX) ||
                Boolean.parseBoolean(this.props.getProperty(Parameters.SANDBOX));


        if (sandbox) {
            MTURK_URL = MTURK_SANDBOX_URL;
            EXTERNAL_HIT = MTURK_SANDBOX_EXTERNAL_HIT;
        } else {
            MTURK_URL = MTURK_PROD_URL;
            EXTERNAL_HIT = MTURK_PROD_EXTERNAL_HIT;
        }

        File cfile = new File(CONFIG);
        File alt = new File(CONFIG+".csv");
        if (! cfile.exists() ) {
            if (alt.exists())
                alt.renameTo(cfile);
            else Runner.LOGGER.warn("ERROR: You have not yet set up the surveyman directory nor AWS keys. Please see the project website for instructions.");
        } else {
            try {
                // make sure we have both names for the access keys in the config file
                Properties config = new Properties();
                config.load(new FileInputStream(CONFIG));
                if (config.containsKey("AWSAccessKeyId") && config.containsKey("AWSSecretKey")) {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(CONFIG, true));
                    bw.newLine();
                    if (! config.containsKey("access_key")) {
                        bw.write("access_key=" + config.getProperty("AWSAccessKeyId"));
                        bw.newLine();
                    }
                    if (! config.containsKey("secret_key")) {
                        bw.write("secret_key=" + config.getProperty("AWSSecretKey"));
                        bw.newLine();
                    }
                    bw.close();
                } else if (config.containsKey("access_key") && config.containsKey("secret_key")) {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(CONFIG, true));
                    bw.newLine();
                    if (! config.containsKey("AWSAccessKeyId")) {
                        bw.write("AWSAccessKeyId="+config.getProperty("access_key"));
                        bw.newLine();
                    }
                    if (! config.containsKey("AWSSecretKey")) {
                        bw.write("AWSSecretKey="+config.getProperty("secret_key"));
                        bw.newLine();
                    }
                    bw.close();
                }
            } catch (IOException io){
                Runner.LOGGER.trace(io);
            }
        }
    }

    public static void dumpSampleProperties()
            throws IOException
    {
        if (!new File(LOCALDIR).exists())
            new File(LOCALDIR).mkdirs();

        PrintWriter writer = new PrintWriter(new FileWriter(PARAMS));
        writer.write(Slurpie.slurp("params.properties"));
        writer.flush();
        writer.close();
    }

    @Override
    public List<String> getBackendHeaders()
    {
        return this.backendHeaders;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MturkLibrary) {
            MturkLibrary that = (MturkLibrary) o;
            if (!this.CONFIG.equals(that.CONFIG)) {
                LOGGER.debug(String.format("Config filenames are not equal (%s vs. %s)", this.CONFIG, that.CONFIG));
                return false;
            } else if (!this.EXTERNAL_HIT.equals(that.EXTERNAL_HIT)) {
                LOGGER.debug(String.format("External HIT URLs not equal (%s vs. %s)", this.EXTERNAL_HIT, that.EXTERNAL_HIT));
                return false;
            } else if (!this.MTURK_URL.equals(that.MTURK_URL)) {
                LOGGER.debug(String.format("Mturk URLs not equal (%s vs. %s)", this.MTURK_URL, that.MTURK_URL));
                return false;
            } else {
                Set<String> thisBackendHeaders = new HashSet<String>(this.backendHeaders);
                Set<String> thatBackendHeaders = new HashSet<String>(that.backendHeaders);
                if (thisBackendHeaders.equals(thatBackendHeaders))
                    return true;
                else {
                    LOGGER.debug(String.format("Backend headers not equal (%s vs. %s)",
                            StringUtils.join(thisBackendHeaders, ","),
                            StringUtils.join(thatBackendHeaders, ",")));
                    return false;
                }
            }
        } else return false;
    }
}


