import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.log4j.*;

import java.io.IOException;

public class TestLog {

    protected final Logger LOGGER = Logger.getRootLogger();

    public String[] testsFiles;
    public char[] separators;
    public boolean[] outcome;
    public final String prefix = String.format("src%1$stest%1$sresources%1$s", AbstractLibrary.fileSep);

    public TestLog()
            throws SurveyException, IOException {
        String[] testData = Slurpie.slurp("test_data").split(System.getProperty("line.separator"));
        this.testsFiles = new String[testData.length];
        this.separators = new char[testData.length];
        this.outcome = new boolean[testData.length];
        for (int i = 0 ; i < testData.length ; i++) {
            String[] stuff = testData[i].split("\\s+");
            this.testsFiles[i] = prefix + stuff[0];
            this.outcome[i] = Boolean.parseBoolean(stuff[2]);
            if (stuff[1].equals(","))
                this.separators[i] = '\u002c';
            else if (stuff[1].equals("\t") || stuff[1].equals("\\t"))
                this.separators[i] = '\u0009';
            else throw new SyntaxException("Unknown delimiter: " + stuff[1]);
        }
    }

    public void init(Class cls){
        LOGGER.setLevel(Level.ALL);
        try {
            FileAppender txtHandler = new FileAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"), String.format("logs/%s.log", cls.getName()));
            txtHandler.setEncoding("UTF-8");
            txtHandler.setAppend(false);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            throw new RuntimeException(io);
        }
    }
}
