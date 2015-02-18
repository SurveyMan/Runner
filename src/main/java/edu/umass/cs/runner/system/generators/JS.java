package edu.umass.cs.runner.system.generators;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.HTMLComponent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.log4j.Logger;

public class JS {
    
    private static final Logger LOGGER = Logger.getLogger("system.mturk");

    private static String makeJS(
            Component preview)
            throws SurveyException,
            IOException
    {
        String loadPreview;
        if (preview instanceof HTMLComponent)
            loadPreview = String.format(" var loadPreview = function () { $('#preview').load('%s'); }; "
                    , ((HTMLComponent) preview).data);
        else loadPreview = " var loadPreview = function () {}; ";
        return loadPreview;
    }

    public static String getJSString(
            Survey survey,
            Component preview)
            throws SurveyException,
            IOException
    {
        String js = "";
        try {
            js = makeJS(preview) + String.format("SurveyMan.display.ready(%s, function() { %s });",
                    survey.jsonize(), Slurpie.slurp(AbstractLibrary.JSSKELETON, true));
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