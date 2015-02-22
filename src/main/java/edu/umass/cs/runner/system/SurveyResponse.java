package edu.umass.cs.runner.system;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.generators.JS;
import edu.umass.cs.runner.system.output.AnswerQuad;
import edu.umass.cs.runner.system.output.AnswerStruct;
import edu.umass.cs.runner.system.output.SurveyResponseStruct;
import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.ISurveyResponseReader;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.input.json.JSONParser;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.StringComponent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONObject;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class SurveyResponse extends AbstractSurveyResponse implements ISurveyResponseReader {

    public static final Logger LOGGER = Logger.getLogger("survey");
    public static final Gensym gensym = new Gensym("sr");
    public static final String dateFormat = "EEE, d MMM yyyy HH:mm:ss Z";

    protected String srid = gensym.next();
    public Record record;
    private String workerId;

    /** otherValues is a map of the key value pairs that are not necessary for quality control,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    public  Map<String, String> otherValues = new HashMap<String, String>();

    // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        this.workerId = wID;
        this.srid = wID;
    }

    @Override
    public String getSrid()
    {
        return this.workerId;
    }

    public SurveyResponse (Survey s, String workerId, String xmlAns, Record record, Map<String, String> ov)
            throws SurveyException, DocumentException, IOException, SAXException, ParserConfigurationException {
        this.workerId = workerId;
        this.record = record;
        this.setResponses(parse(s, xmlAns, ov));
    }

    @Override
    public boolean surveyResponseContainsAnswer(List<Component> components) {
        for (IQuestionResponse qr : this.getAllResponses()) {
            for (OptTuple optTuple : qr.getOpts()) {
                if (components.contains(optTuple.c))
                    return true;
            }
        }
        return false;
    }

    @Override
    public Map<String,IQuestionResponse> resultsAsMap() {
        HashMap<String,IQuestionResponse> res = new HashMap<String, IQuestionResponse>();
        for(IQuestionResponse resp : this.getAllResponses()) {
            assert resp.getQuestion().data!=null : resp.getQuestion().quid;
            res.put(resp.getQuestion().quid, resp);
        }
        return Collections.unmodifiableMap(res);
    }


    public List<AbstractSurveyResponse> readSurveyResponses(Survey s, String filename) throws SurveyException {

        List<AbstractSurveyResponse> responses = null;

        if (new File(filename).isFile()) {

            try {
                responses = readSurveyResponses(s, new FileReader(filename));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (new File(filename).isDirectory()) {

            responses = new ArrayList<AbstractSurveyResponse>();

            for (File f : new File(filename).listFiles()) {
                try {
                    responses.addAll(readSurveyResponses(s, new FileReader(f)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } else throw new RuntimeException("Unknown file or directory: "+filename);

        return responses;
    }

    @Override
    public List<AbstractSurveyResponse> readSurveyResponses(
            Survey s,
            Reader r)
            throws SurveyException
    {

        List<AbstractSurveyResponse> responses = new LinkedList<AbstractSurveyResponse>();

        final CellProcessor[] cellProcessors = s.makeProcessorsForResponse();

        try{
            ICsvMapReader reader = new CsvMapReader(r, CsvPreference.STANDARD_PREFERENCE);
            final String[] header = reader.getHeader(true);
            Map<String, Object> headerMap;
            AbstractSurveyResponse sr = null;
            while ((headerMap = reader.read(header, cellProcessors)) != null) {
                // loop through one survey response (i.e. per responseid) at a time
                if ( sr == null || !sr.getSrid().equals(headerMap.get("responseid"))){
                    if (sr!=null)
                        // add this to the list of responses and create a new one
                        responses.add(sr);
                    sr = new SurveyResponse((String) headerMap.get("workerid"));
                    sr.setSrid((String) headerMap.get("responseid"));

                }
                // fill out the individual question responses
                IQuestionResponse questionResponse = new QuestionResponse(s, (String) headerMap.get("questionid"), (Integer) headerMap.get("questionpos"));
                for (IQuestionResponse qr : sr.getAllResponses())
                    if (qr.getQuestion().quid.equals((String) headerMap.get("questionid"))) {
                        // if we already have a QuestionResponse object matching this id, set it
                        questionResponse = qr;
                        break;
                    }
                Component c;
                if (!Question.customQuestion(questionResponse.getQuestion().quid))
                    c = questionResponse.getQuestion().getOptById((String) headerMap.get("optionid"));
                else c = new StringComponent((String) headerMap.get("optionid"), -1, -1);
                Integer i = (Integer) headerMap.get("optionpos");
                questionResponse.getOpts().add(new OptTuple(c,i));
                sr.getAllResponses().add(questionResponse);
            }
            reader.close();
            return responses;
        } catch (IOException io) {
            io.printStackTrace();
        }
        return null;
    }

    public static ArrayList<IQuestionResponse> parse(Survey s, String ansXML, Map<String, String> otherValues)
            throws DocumentException, SurveyException, ParserConfigurationException, IOException, SAXException {
        ArrayList<IQuestionResponse> retval = new ArrayList<IQuestionResponse>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(ansXML.getBytes("utf-8"))));
        NodeList answers = doc.getElementsByTagName("Answer");
        for ( int i = 0 ; i < answers.getLength() ; i++ ) {
            Node n = answers.item(i);
            Element e = (Element) n;
            String quid = e.getElementsByTagName("QuestionIdentifier").item(0).getTextContent();
            String opts = e.getElementsByTagName("FreeText").item(0).getTextContent();
            QuestionResponse questionResponse;
            if (quid.equals("commit"))
                continue;
            else if (!quid.startsWith("q")) {
                questionResponse = new QuestionResponse();
                questionResponse.add(quid, new OptTuple(new StringComponent(opts, -1, -1), -1), otherValues);
            } else {
                questionResponse = new QuestionResponse(s.getQuestionById(quid));
                String[] optionStuff = opts.split("\\|");
                for (String optionJSON : optionStuff) {
                    try {
                        questionResponse.add(new JSONObject(optionJSON), s, otherValues);
                    } catch (Exception ise) {
                        System.err.println(String.format("JSON parse error: %s\nGenerating alternate entry.", ise.getMessage()));
                        // this is a hack
                        LOGGER.info(ise);
                        LOGGER.info(optionJSON);
                        questionResponse.add(quid, new OptTuple(new StringComponent(optionJSON, -1, -1), -1), null);
                    }
                }
                retval.add(questionResponse);
            }
        }
        return retval;
    }

    public SurveyResponseStruct makeStruct()
    {
        double score = this.getScore();
        double threshold = this.getThreshold();
        List<AnswerQuad> answerQuads = new ArrayList<AnswerQuad>();
        for (IQuestionResponse questionResponse : this.getAllResponses()) {
            for (OptTuple optTuple : questionResponse.getOpts())
                answerQuads.add(new AnswerQuad(
                        questionResponse.getQuestion().quid,
                        optTuple.c.getCid(),
                        questionResponse.getIndexSeen(),
                        optTuple.i)
                );
        }
        for (Map.Entry<String, String> s : this.otherValues.entrySet())
            answerQuads.add(new AnswerQuad(s.getKey(), s.getValue(), -1, -1));
        return new SurveyResponseStruct(
                this.getSrid(),
                new AnswerStruct(answerQuads),
                score,
                threshold,
                score > threshold);
    }

}
