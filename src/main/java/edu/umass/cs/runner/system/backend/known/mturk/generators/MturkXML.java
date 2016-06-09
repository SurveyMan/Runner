package edu.umass.cs.runner.system.backend.known.mturk.generators;

import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.generators.HTML;
import edu.umass.cs.runner.system.backend.known.mturk.MturkLibrary;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.*;

public class MturkXML {

    public static final int maxQuestionXMLLength = 131072;

    public static String getXMLString(QCMetrics qcMetrics) throws SurveyException {
        String retval;
        try {
            Record record = new Record(qcMetrics, new MturkLibrary(), KnownBackendType.MTURK);
            retval = String.format(Slurpie.slurp(MturkLibrary.XMLSKELETON), HTML.getHTMLString(record, new MturkHTML()));
            if (retval.length() > maxQuestionXMLLength)
                throw new MaxXMLLengthException(retval.length());
        } catch (FileNotFoundException e1) {
            throw new SurveyException(e1.getMessage()){};
        } catch (IOException e2) {
            throw new SurveyException(e2.getMessage()){};
        }
        return retval;
    }


    public static class MaxXMLLengthException extends SurveyException {
        public MaxXMLLengthException(int stringLength){
            super(String.format("Question length is %d bytes, exceeds max length of %d bytes by %d bytes."
                    , stringLength
                    , MturkXML.maxQuestionXMLLength
                    , stringLength - MturkXML.maxQuestionXMLLength));
        }
    }

}
