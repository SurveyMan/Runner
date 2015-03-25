package edu.umass.cs.runner.system.generators;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.HTMLComponent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.log4j.Logger;

public class JS {
    
    private static final Logger LOGGER = Logger.getLogger("system.mturk");

    private static String makePreview(
            Component preview)
            throws SurveyException,
            IOException
    {
        String loadPreview;
        if (preview instanceof HTMLComponent)
            loadPreview = String.format(" $('#preview').append('%s'); "
                    , ((HTMLComponent) preview).data);
        else loadPreview = "";
        return loadPreview;
    }

    public static String getJSString(
            KnownBackendType knownBackendType,
            Survey survey,
            Component preview)
            throws SurveyException,
            IOException
    {
        String js = "";
        try {
            js = String.format("SurveyMan.display.ready(" +
                            "%b, " +
                            "%s, " +
                            "function () { %s }, " +
                            "function() { %s }" +
                            ");",
                    knownBackendType.equals(KnownBackendType.MTURK),
                    survey.jsonize(),
                    makePreview(preview),
                    Slurpie.slurp(AbstractLibrary.JSSKELETON, true));
        } catch (FileNotFoundException ex) {
            LOGGER.fatal(ex);
            ex.printStackTrace();
            System.exit(-1);
        } catch (IOException ex) {
            LOGGER.fatal(ex);
            ex.printStackTrace();
            System.exit(-1);
        }
        return js;
        //return new ClosureJavaScriptCompressor().compress(js);
    }
}