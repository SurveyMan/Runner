package edu.umass.cs.runner.system.localhost.generators;

import edu.umass.cs.runner.IHTML;
import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.generators.HTML;
import edu.umass.cs.runner.system.localhost.LocalLibrary;
import edu.umass.cs.runner.system.localhost.Server;

import java.io.File;

/**
 * Created by etosch on 2/13/14.
 */
public class LocalHTML implements IHTML {

    public static final int port = Server.frontPort;

    public String getHTMLString() {
        return HTML.COMMON_STRINGS + getSetAssignmentId();
    }

    private String getSetAssignmentId(){
        return "<script type=\"text/javascript\">\n"+
                " $.ajaxSetup({async:false});\n" +
                "var turkSetAssignmentID = function () { $.get(\"assignmentId\", function(_aid) { " +
                "console.log(\"Just pulled assignment Id : \" + _aid); " +
                "document.getElementById(\"assignmentId\").value = _aid.trim(); " +
                "aid = _aid;" +
                "}); }; \n"
                + "</script>\n";
    }

    public String getActionForm(Record record) {
        return "";
    }

    public LocalHTML(){}
}
