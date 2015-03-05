package edu.umass.cs.runner.system.generators;

import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.IHTML;
import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.input.AbstractLexer;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.HTMLComponent;
import edu.umass.cs.surveyman.survey.StringComponent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import edu.umass.cs.runner.Record;

import java.io.*;
import java.net.MalformedURLException;

public class HTML {

    public static final String COMMON_STRINGS =
            "<script type=\"text/javascript\" src=\"//cdnjs.cloudflare.com/ajax/libs/require.js/2.1.10/require.js\" ></script>"
            + "<script type=\"text/javascript\" src=\"https://jqueryjs.googlecode.com/files/jquery-1.3.2.min.js\"></script>\n"
            + "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/seedrandom/2.3.4/seedrandom.min.js\"></script>\n"
            + "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.6.0/underscore.js\"></script>\n"
            + "<script type=\"text/javascript\" src=\"https://surveyman.github.io/surveyman.js/survey.js\"></script>\n"
            + "<script type=\"text/javascript\" src=\"https://surveyman.github.io/surveyman.js/display.js\"></script>\n"
            + "<script type=\"text/javascript\" src=\"https://surveyman.github.io/surveyman.js/interpreter.js\"></script>\n";

    private static String stringify()
            throws SurveyException,
            MalformedURLException
    {
        return "<div name=\"question\" hidden>"
                    + "<p class=\"question\"></p>"
                    + "<p class=\"answer\"></p>"
                    + "</div>";
    }

    private static String stringifyPreview(
            Component c)
            throws SurveyException
    {
        String baseString = Component.html(c);
        return String.format("<div id=\"preview\" %s>%s</div>"
                , ((c instanceof HTMLComponent) ? "onload=\"loadPreview();\""
                                              : "")
                , ((c instanceof StringComponent) ? CSVLexer.htmlChars2XML(baseString) : ""));
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

    public static String cleanedPreview(
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
            Component preview = AbstractParser.parseComponent(HTMLComponent.isHTMLComponent(strPreview) ? AbstractLexer.xmlChars2HTML(strPreview) : strPreview, -1, -1);
            html = String.format(Slurpie.slurp(AbstractLibrary.HTMLSKELETON)
                    , record.survey.encoding
                    , JS.getJSString(record.survey, preview)
                    , stringifyPreview(preview)
                    , stringify()
                    , backendHTML.getActionForm(record)
                    , record.survey.source
                    , record.outputFileName
                    , backendHTML.getHTMLString()
                    , Slurpie.slurp(AbstractLibrary.CUSTOMCSS, true)
            );
        } catch (FileNotFoundException ex) {
            Runner.LOGGER.fatal(ex);
            System.exit(-1);
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