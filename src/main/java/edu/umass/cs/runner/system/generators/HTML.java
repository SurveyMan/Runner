package edu.umass.cs.runner.system.generators;

import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.IHTML;
import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.system.backend.KnownBackendType;
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

    private static String oauth_headers(String clientid) {
        return "    <meta name=\"google-signin-scope\" content=\"profile email\">\n" +
                "    <meta name=\"google-signin-client_id\" content=\"" +
                clientid +
                ".apps.googleusercontent.com\">\n" +
                "    <script src=\"https://apis.google.com/js/platform.js\" async defer></script>";
    }

    private static String oauth_body() {
        return " <script>" +
                "turkSetAssignmentID = function () {" +
                "};\n" +
                "</script>" +
                "<div class=\"g-signin2\" data-onsuccess=\"onSignIn\" data-theme=\"dark\"></div>\n" +
                "    <script>\n" +
                "      function onSignIn(googleUser) {\n" +
                "        // Useful data for your client-side scripts:\n" +
                "        var profile = googleUser.getBasicProfile();\n" +
                "        console.log(\"ID: \" + profile.getId()); // Don't send this directly to your server!\n" +
                "        console.log('Full Name: ' + profile.getName());\n" +
                "        console.log('Given Name: ' + profile.getGivenName());\n" +
                "        console.log('Family Name: ' + profile.getFamilyName());\n" +
                "        console.log(\"Image URL: \" + profile.getImageUrl());\n" +
                "        console.log(\"Email: \" + profile.getEmail());\n" +
                "\n" +
                "        // The ID token you need to pass to your backend:\n" +
                "        var id_token = googleUser.getAuthResponse().id_token;\n" +
                "        console.log(\"ID Token: \" + id_token);\n" +
                "        document.getElementById(\"assignmentId\").value = profile.getId().trim();\n" +
                "        console.log(document.getElementById(\"assignmentId\").value);" +
                "        aid = profile.getId().trim();\n" +
                "      };\n" +
                "    </script>" +
                "<a href=\"#\" onclick=\"signOut();\" id=\"signout\">Sign out</a>\n" +
                "<script>\n" +
                "  function signOut() {\n" +
                "    var auth2 = gapi.auth2.getAuthInstance();\n" +
                "    auth2.signOut().then(function () {\n" +
                "      console.log('User signed out.');\n" +
                "    });\n" +
                "  }\n" +
                "  $.hide(\"#signout\");\n" +
                "</script>";
    }

    private static boolean use_oauth(Record record) {
        return record.backendType.equals(KnownBackendType.LOCALHOST) &&
                record.library.use_oauth(record);
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
            SurveyDatum preview = AbstractParser.parseComponent(
                    HTMLDatum.isHTMLComponent(strPreview) ? AbstractLexer.xmlChars2HTML(strPreview) : strPreview,
                    -1, -1, -1);
            html = String.format(Slurpie.slurp(AbstractLibrary.HTMLSKELETON)
                    , record.survey.encoding
                    , JS.getJSString(record.backendType, record.survey, preview)
                    , stringifyPreview(preview)
                    , stringify()
                    , backendHTML.getActionForm(record)
                    , record.survey.source
                    , record.outputFileName
                    , backendHTML.getHTMLString()
                    , Slurpie.slurp(AbstractLibrary.CUSTOMCSS, true)
                    , use_oauth(record) ? oauth_headers(record.library.getClientId(record)) : ""
                    , use_oauth(record) ? oauth_body() : ""
            );
        } catch (IOException ex) {
            Runner.LOGGER.fatal(ex);
            System.exit(-1);
        }
        try{
            spitHTMLToFile(html, record.survey);
        } catch (IOException io) {
            Runner.LOGGER.warn(io);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return html;
//        return (new HtmlCompressor()).compress(html);
    }
}