package edu.umass.cs.runner;

import edu.umass.cs.runner.system.SurveyResponse;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.survey.HTMLDatum;
import edu.umass.cs.surveyman.survey.InputOutputKeys;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.Survey;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.io.RuntimeIOException;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ResponseWriter {

    public static String[] defaultHeaders = new String[]{"responseid", "workerid", "surveyid"
            , "questionid", "questiontext", "questionpos"
            , "optionid", "optiontext", "optionpos"};
    public static final String sep = ",";
    public static final String newline = "\r\n";
    public static final CellProcessor[] defaultProcessors = new CellProcessor[] {
            new NotNull(), // responseid
            new NotNull(), // workerid
            new NotNull(), // surveyid
            new NotNull(), // questionid
            new NotNull(), // questiontext
            new NotNull(), // optionid
            new NotNull(), // optiontext
            new NotNull() // optionpos
    };

    public final Survey survey;
    public final List<String> backendHeaders;
    public final File outputFile;
    private boolean writtenHeaders;

    public ResponseWriter(Record record) {
        this.survey = record.survey;
        this.backendHeaders = record.library.getBackendHeaders();
        this.outputFile = new File(record.outputFileName);
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(this.outputFile, true));
            writeHeaders(pw);
            this.writtenHeaders = true;
            pw.close();
        } catch (IOException io) {
            throw new RuntimeIOException(io);
        }
    }

    public List<String> getHeaders() {
        List<String> s = new ArrayList<>();

        // default headers
        s.add(defaultHeaders[0]);
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
            s.append(String.format("%s%s", sep, InputOutputKeys.CORRELATION));

        s.append("\r\n");
        Runner.LOGGER.info("headers:" + s.toString());
        return s;
    }

    private CellProcessor[] getCellProcessors() {
        // returns all of the cell processors (including the custom ones and the mturk backend ones
        return new CellProcessor[]{};
    }

    public void writeHeaders(PrintWriter pw) throws IOException
    {
        List<String> headers = getHeaders();
        // this isn't using cell processors right now, but should maybe be updated to do so later
        pw.write(StringUtils.join(headers, ","));
        pw.flush();
    }

    public void writeResponse(PrintWriter pw, SurveyResponse sr) throws IOException {
        // TODO: write response using cell processors.
        sr.setRecorded(true);
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
