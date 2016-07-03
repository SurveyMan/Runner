package edu.umass.cs.runner.system.backend.known.localhost;

import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.system.backend.known.localhost.server.WebHandler;
import edu.umass.cs.runner.system.backend.known.localhost.server.WebServer;
import edu.umass.cs.runner.system.backend.known.localhost.server.WebServerException;
import edu.umass.cs.runner.utils.Slurpie;
import edu.umass.cs.surveyman.input.AbstractLexer;
import edu.umass.cs.surveyman.utils.Gensym;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Server {

    static final String RESPONSES = "responses";
    private static final String ASSIGNMENTID = "assignmentId";
    private static final String SURVEYMAN_JS = "surveyman.main.js";

    static class IdResponseTuple {
        public String id, xml;
        IdResponseTuple(String id, String xml) {
            this.id = id; this.xml = xml;
        }
        String jsonize() {
            return String.format("{\"workerid\" : \"%s\", \"answer\" : \"%s\"}", id, AbstractLexer.xmlChars2HTML(xml));
        }
    }

    public static volatile int frontPort = 8000;
    static boolean serving = false;
    private static int requests = 0;
    private static Gensym gensym = new Gensym("a");
    private final static List<IdResponseTuple> newXmlResponses = new ArrayList<>();
    private final static List<IdResponseTuple> oldXmlResponses = new ArrayList<>();

    private static WebServer server;

    public static void startServe() throws WebServerException {
        server = WebServer.start(frontPort, new WebHandler() {
            @Override
            public void handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {

                requests++;
                httpResponse.addHeader("Access-Control-Allow-Origin:", "http://surveyman.github.io");

                String method = httpRequest.getMethod();
                String httpPath = httpRequest.getPathInfo();

                Runner.LOGGER.info("HTTP Request: "+method+" "+httpPath);

                String response = "";
                if("GET".equals(method)) {
                    if (httpPath.endsWith(RESPONSES))
                        response = getJsonizedNewResponses();
                    else if (httpPath.endsWith(ASSIGNMENTID))
                        response = gensym.next();
                    else if (httpPath.endsWith(SURVEYMAN_JS)) {
                        // since we are serving out of the logs folder
                        // http://stackoverflow.com/questions/33829573/how-to-get-embedded-jetty-serving-html-files-from-a-jar-not-a-war
                        response = edu.umass.cs.surveyman.utils.Slurpie.slurp(SURVEYMAN_JS);
                        Runner.LOGGER.debug("Read response: " + response);
                    } else {
                        String path = httpPath.replace("/", AbstractLibrary.fileSep).substring(1);
                        try {
                            response = Slurpie.slurp(path);
                        } catch (IOException e) {
                            httpResponse.sendError(404, "Not Found");
                            Runner.LOGGER.warn(e);
                            return;
                        }
                    }
                } else if("POST".equals(method)) {
                    Map<String,String[]> formParams = (Map<String,String[]>) httpRequest.getParameterMap();
                    IdResponseTuple xml = convertToXML(formParams);

                    synchronized (newXmlResponses) {
                        newXmlResponses.add(xml);
                    }

                    response = Slurpie.slurp("thanks.html");
                } else {
                    httpResponse.sendError(400, "Bad Request");
                    return;
                }

                // send response body
                httpResponse.setStatus(200);
                httpResponse.setContentType("text/html");
                PrintWriter out = httpResponse.getWriter();
                out.println(response);
                out.close();
            }
        });
        serving = true;
    }

    public static void endServe() throws WebServerException {
        serving = false;
        server.stop();
    }

    private static String getJsonizedNewResponses() {
        synchronized (newXmlResponses) {
            Iterator<IdResponseTuple> tupes = newXmlResponses.iterator();
            StringBuilder sb = new StringBuilder();
            if (tupes.hasNext()) {
                IdResponseTuple tupe = tupes.next();
                sb.append(tupe.jsonize());
                tupes.remove();
                oldXmlResponses.add(tupe);
            } else return "";
            while (tupes.hasNext()) {
                IdResponseTuple tupe = tupes.next();
                sb.append(String.format(", %s", tupe.jsonize()));
            }
            for (IdResponseTuple tupe : oldXmlResponses) {
                if (newXmlResponses.contains(tupe))
                    newXmlResponses.remove(tupe);
            }
            return String.format("[%s]", sb.toString());
        }
    }

    public static IdResponseTuple convertToXML(Map<String,String[]> postParams) {
        String assignmentId = "";
        // while the answer doesn't need to go be converted to MturkXML, this is set up to double as an offline simulator for mturk.
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><QuestionFormAnswers xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionFormAnswers.xsd\">");
        for(Map.Entry<String,String[]> entry: postParams.entrySet()) {
            String key = entry.getKey();
            String value = StringUtils.join(entry.getValue(), '|');
            xml.append("<Answer><QuestionIdentifier>")
                    .append(key).append("</QuestionIdentifier><FreeText>")
                    .append(value).append("</FreeText></Answer>");

            if (key.equals("assignmentId"))
                assignmentId = value;
        }
        xml.append("</QuestionFormAnswers>");
        return new IdResponseTuple(assignmentId, xml.toString());
    }

    public static boolean endSurvey() throws WebServerException {
        endServe();
        server = WebServer.start(frontPort, new WebHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
                PrintWriter writer = response.getWriter();
                writer.println(Slurpie.slurp("survey_closed.html"));
                writer.close();
            }
        });
        return true;
    }
}
