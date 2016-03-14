package edu.umass.cs.runner;

import edu.umass.cs.runner.system.SurveyResponse;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.survey.HTMLDatum;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.Survey;

import java.util.*;

public class ResponseWriter {

    public static String[] defaultHeaders = new String[]{"responseid", "workerid", "surveyid"
            , "questionid", "questiontext", "questionpos"
            , "optionid", "optiontext", "optionpos"};
    public static final String sep = ",";
    public static final String newline = "\r\n";


    public static String outputHeaders(Survey survey, List<String> backendHeaders) {
        StringBuilder s = new StringBuilder();

        // default headers
        s.append(defaultHeaders[0]);
        for (String header : Arrays.asList(defaultHeaders).subList(1, defaultHeaders.length))
            s.append(String.format("%s%s", sep, header));

        // user-provided other headers
        if (survey.otherHeaders != null)
            for (String header : survey.otherHeaders)
                s.append(String.format("%s%s", sep, header));

        // mturk-provided other headers
        Collections.sort(backendHeaders);
        for (String key : backendHeaders)
            s.append(String.format("%s%s", sep, key));

        //correlation
        if (survey.correlationMap != null && !survey.correlationMap.isEmpty())
            s.append(String.format("%s%s", sep, AbstractParser.CORRELATION));

        s.append("\r\n");
//        Runner.LOGGER.info("headers:" + s.toString());
        return s.toString();
    }

    private static String outputQuestionResponse(
            Survey survey,
            IQuestionResponse qr,
            SurveyResponse sr)
    {

        StringBuilder retval = new StringBuilder();

        // construct actual question text
        StringBuilder qtext = new StringBuilder();
        qtext.append(String.format("%s", qr.getQuestion().data.toString().replaceAll("\"", "\"\"")));
        qtext.insert(0, "\"");
        qtext.append("\"");

        assert qr.getOpts().size() > 0;

        // response options
        for (OptTuple opt : qr.getOpts()) {

            // construct actual option text
            String otext = "";
            if (opt.c instanceof HTMLDatum)
                otext = ((HTMLDatum) opt.c).data.toString();
            else if (opt.c instanceof StringDatum && ! opt.c.isEmpty())
                otext = ((StringDatum) opt.c).data.toString();
            otext = otext.replaceAll("\"", "\"\"");
            otext = "\"" + otext + "\"";

            //construct line of contents
            StringBuilder toWrite = new StringBuilder("%1$s");
            for (int i = 1 ; i < defaultHeaders.length ; i++)
                toWrite.append(String.format("%s%%%d$s", sep, i+1));
            retval.append(String.format(toWrite.toString()
                    , sr.getSrid()
                    , sr.getSrid()
                    , survey.sid
                    , qr.getQuestion().id
                    , qtext.toString()
                    , qr.getIndexSeen()
                    , opt.c.getId()
                    , otext
                    , opt.i));

            // add contents for user-defined headers
            if (survey.otherHeaders!=null && survey.otherHeaders.length > 0) {
                //retval.append(survey.otherHeaders[0]);
                for (int i = 0 ; i < survey.otherHeaders.length ; i++){
                    String header = survey.otherHeaders[i];
                    retval.append(String.format("%s\"%s\"", sep, qr.getQuestion().otherValues.get(header)));
                }
            }

            // add contents for system-defined headers
            Map<String, String> backendHeaders = sr.otherValues;
            if (!backendHeaders.isEmpty()) {
                List<String> keys = new ArrayList<String>(backendHeaders.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    retval.append(String.format("%s\"%s\"", sep, backendHeaders.get(key)));
                }
            }

            // add correlated info
            if (survey.correlationMap != null && !survey.correlationMap.isEmpty())
                retval.append(String.format("%s%s", sep, survey.getCorrelationLabel(qr.getQuestion())));

            retval.append(newline);

        }

        //retval.append(newline);
        return retval.toString();

    }

    public static String outputSurveyResponse(
            Survey survey,
            SurveyResponse sr)
    {

        StringBuilder retval = new StringBuilder();

        //assert sr.getAllResponses().size() > 0 : "Cannot have 0 responses to a survey!!";

        for (IQuestionResponse qr : sr.resultsAsMap().values())
            retval.append(outputQuestionResponse(survey, qr, sr));

        //assert retval.length() != 0 : "Cannot have a survey response of length 0!!";

        return retval.toString();
    }

    public static String outputSurveyResponses(Survey survey, List<SurveyResponse> surveyResponses) {

        StringBuilder retval = new StringBuilder();

        for (SurveyResponse sr : surveyResponses)
            retval.append(outputSurveyResponse(survey, sr));

        return retval.toString();

    }


}
