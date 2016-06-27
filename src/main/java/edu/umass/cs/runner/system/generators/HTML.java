package edu.umass.cs.runner.system.generators;

import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.IHTML;
import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.input.AbstractLexer;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.survey.HTMLDatum;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import edu.umass.cs.runner.Record;

import java.io.*;
import java.net.MalformedURLException;

public class HTML {

    public static final String REMOTE_REFERENCE = jsReference("https://surveyman.github.io/surveyman.js/surveyman.main.js");
    public static final String LOCAL_REFERENCE =  jsReference("../surveyman.js/surveyman.main.js");

    private static String jsReference(String src) {
        return String.format("<script type=\"text/javascript\" src=\"%s\"></script>\n", src);
    }

    private static String stringify()
            throws SurveyException,
            MalformedURLException
    {
        return "<div name=\"question\" hidden>"
                    + "<p class=\"question\"></p>"
                    + "<p class=\"answer\"></p>"
                    + "</div>";
    }

    private static String stringifyPreview(SurveyDatum c) throws SurveyException
    {
        String baseString = SurveyDatum.html(c);
        return String.format("<div id=\"preview\" hidden>%s</div>"
                , ((c instanceof StringDatum) ? CSVLexer.htmlChars2XML(baseString) : ""));
    }

    public static void spitHTMLToFile(
            String html,
            Survey survey)
            throws IOException,
            SurveyException,
            InstantiationException,
            IllegalAccessException
    {
        String htmlFileName = Record.getHtmlFileName(survey);
        Runner.LOGGER.info(String.format("Source html found at %s", htmlFileName));
        BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFileName));
        bw.write(html);
        bw.close();
    }

    private static String cleanedPreview(
            Record record)
    {
        String preview = record.library.props.getProperty("splashpage", "");
        Document doc = Jsoup.parse(preview);
        Element body = doc.body();
        return body.html();
    }

    public static String getHTMLString(
            Record record,
            IHTML backendHTML)
            throws SurveyException
    {
        String html = "";
        try {
            assert record!=null;
            assert(record.library!=null);
            assert(record.library.props!=null);
            String strPreview = cleanedPreview(record);
            String breakoffMessage = record.breakoffMessage;
            SurveyDatum preview = AbstractParser.parseComponent(
                    HTMLDatum.isHTMLComponent(strPreview) ? AbstractLexer.xmlChars2HTML(strPreview) : strPreview,
                    -1, -1, -1);
            html = String.format(Slurpie.slurp(AbstractLibrary.HTMLSKELETON)
                    , record.survey.encoding
                    , JS.getJSString(record.backendType, record.survey, preview, breakoffMessage)
                    , stringifyPreview(preview)
                    , stringify()
                    , backendHTML.getActionForm(record)
                    , record.survey.source
                    , record.outputFileName
                    , backendHTML.getHTMLString()
                    , Slurpie.slurp(AbstractLibrary.CUSTOMCSS, true)
            );
        } catch (IOException ex) {
            Runner.LOGGER.fatal(ex);
            System.exit(-1);
        }
        try{
            spitHTMLToFile(html, record.survey);
        } catch (IOException io) {
            Runner.LOGGER.warn(io);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return html;
//        return (new HtmlCompressor()).compress(html);
    }
}