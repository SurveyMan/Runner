package edu.umass.cs.runner;

import edu.umass.cs.runner.system.SurveyResponse;
import edu.umass.cs.runner.system.backend.KnownBackendType;
import edu.umass.cs.runner.system.backend.known.mturk.MturkLibrary;
import edu.umass.cs.runner.system.backend.known.mturk.generators.MturkHTML;
import edu.umass.cs.runner.system.backend.known.mturk.generators.MturkXML;
import edu.umass.cs.runner.system.generators.HTML;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.qc.RandomRespondent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import junit.framework.Assert;
import org.apache.commons.lang.ObjectUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

@RunWith(JUnit4.class)
public class SystemTest extends TestLog {

    public SystemTest() throws Exception {
    }

    @Test
    public void testMturkHTMLGenerator()
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException
    {
        try{
            for ( int i = 0 ; i < testsFiles.length ; i++ ) {
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
                Survey survey = csvParser.parse();
                MturkLibrary.dumpSampleProperties();
                Record record = new Record(survey, new MturkLibrary(), KnownBackendType.MTURK);
                HTML.getHTMLString(record, new MturkHTML());
                LOGGER.info(testsFiles[i]+" generated IHTML successfully.");
            }
        } catch (SurveyException se) {
            LOGGER.fatal(se);
        }
    }

    @Test
    public void testXMLGenerator() throws Exception {
        try{
            for (int i = 0 ; i < testsFiles.length ; i++) {
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
                MturkXML.getXMLString(csvParser.parse());
                LOGGER.info(testsFiles[i]+" generated IHTML successfully.");
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testColumnPipeline() throws Exception {
        for (int i = 0 ; i < testsFiles.length ; i++) {
            try {
                System.out.println("File:"+testsFiles[i]);
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
                Survey survey = csvParser.parse();
                RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
                String headers = ResponseWriter.outputHeaders(survey, new ArrayList<String>());
                String output = ResponseWriter.outputSurveyResponse(survey, new SurveyResponse(rr.getResponse()));
                new SurveyResponse("").readSurveyResponses(survey, new StringReader(headers + output));
            } catch (SurveyException se) {
                if (super.outcome[i])
                    throw se;
            } catch (NullPointerException npe) {
                System.out.println(String.format("Were we expecting survey %s to succeed? %b", super.testsFiles[i], super.outcome[i]));
                if (super.outcome[i])
                    throw npe;
                else System.out.println("THIS NEEDS TO FAIL GRACEFULLY");
            }
        }
    }

    @Test
    public void testSerialize()
            throws SurveyException, IOException, NoSuchMethodException, IllegalAccessException,
                   InvocationTargetException, ClassNotFoundException
    {
        CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[0], String.valueOf(separators[0])));
        Survey survey = csvParser.parse();
        MturkLibrary.dumpSampleProperties();
        Record record = new Record(survey, new MturkLibrary(), KnownBackendType.MTURK);
        String serializedFileName = record.serializeRecord();
        Record deserializedRecord = Record.deserializeRecord(serializedFileName);
        try {
            Assert.assertEquals(record, deserializedRecord);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            LOGGER.fatal(npe);
        }
    }

//    @Test
//    public void testCorrelatedPipeline() throws Exception {
//        for (int i = 0 ; i < testsFiles.length ; i++) {
//            try {
//                System.out.println("File:"+testsFiles[i]);
//                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
//                Survey survey = csvParser.parse();
//                if (!survey.correlationMap.isEmpty()) {
//                    System.out.println("input specifies correlations "+survey.correlationMap.entrySet());
//                    RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
//                    String headerString = ResponseWriter.outputHeaders(survey);
//                    assert(headerString.contains(Survey.CORRELATION));
//                    String[] headers = headerString.split(",");
//                    // write a function to actually parse in the correlations and check against correlationMap
//                }
//            } catch (SurveyException se) {
//                if (super.outcome[i])
//                    throw se;
//            } catch (NullPointerException npe) {
//            System.out.println(String.format("Were we expecting survey %s to succeed? %b", super.testsFiles[i], super.outcome[i]));
//            if (super.outcome[i])
//                throw npe;
//            else System.out.println("THIS NEEDS TO FAIL GRACEFULLY");
//        }
//
//    }
//        System.out.println("Success");
//

}