package edu.umass.cs.runner.system.generators;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.github.fge.jsonschema.exceptions.ProcessingException;
//import com.googlecode.htmlcompressor.compressor.ClosureJavaScriptCompressor;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.HTMLComponent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.log4j.Logger;
import edu.umass.cs.runner.Library;

public class JS {
    
    private static final Logger LOGGER = Logger.getLogger("system.mturk");

    private static String makeLoadPreview(Component preview) {
        return String.format(" var loadPreview = function () { $('#preview').load('%s'); }; "
                , ((HTMLComponent) preview).data);
    }

    private static String makeJS(Survey survey, Component preview) throws SurveyException, IOException, ProcessingException {
        String json = "var jsonizedSurvey = " + survey.jsonize() + ";";
        String loadPreview;
        if (preview instanceof HTMLComponent)
            loadPreview = makeLoadPreview(preview);
        else loadPreview = " var loadPreview = function () {}; ";
        return String.format("%s\n%s"
                , loadPreview
                , json
        );
    }

    public static String getJSString(Survey survey, Component preview) throws SurveyException, IOException {
        String js = "";
        try {
            String temp = String.format("var customInit = function() { %s };", Slurpie.slurp(Library.JSSKELETON, true)) ;
            js = makeJS(survey, preview) + temp;
        } catch (FileNotFoundException ex) {
            LOGGER.fatal(ex);
            ex.printStackTrace();
            System.exit(-1);
        } catch (IOException ex) {
            LOGGER.fatal(ex);
            ex.printStackTrace();
            System.exit(-1);
        } catch (ProcessingException e) {
            LOGGER.fatal(e);
            e.printStackTrace();
            System.exit(-1);
        }
        //return new ClosureJavaScriptCompressor().compress(js);
        return js;
    };
}