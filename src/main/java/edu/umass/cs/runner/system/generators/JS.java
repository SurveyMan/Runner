package edu.umass.cs.runner.system.generators;

import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.survey.HTMLDatum;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.log4j.Logger;

import java.io.IOException;

public class JS {
    
    private static final Logger LOGGER = Logger.getLogger("system.generators");

    private static String makePreview(
            SurveyDatum preview)
            throws SurveyException,
            IOException
    {
        String loadPreview;
        if (preview instanceof HTMLDatum)
            loadPreview = String.format(" $('#preview').append('%s'); "
                    , ((HTMLDatum) preview).data);
        else loadPreview = "";
        return loadPreview;
    }

    public static String getJSString(
            KnownBackendType knownBackendType,
            Survey survey,
            SurveyDatum preview)
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
        } catch (IOException ex) {
            LOGGER.fatal(ex);
            ex.printStackTrace();
            System.exit(-1);
        }
        return js;
        //return new ClosureJavaScriptCompressor().compress(js);
    }
}