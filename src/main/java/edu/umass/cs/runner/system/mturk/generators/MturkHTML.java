package edu.umass.cs.runner.system.mturk.generators;

import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.IHTML;
import edu.umass.cs.runner.system.generators.HTML;

public class MturkHTML implements IHTML {

    public String getHTMLString() {
        return HTML.COMMON_STRINGS +
                "<script type=\"text/javascript\" src=\"https://s3.amazonaws.com/mturk-public/externalHIT_v1.js\">" +
                "</script>";
    }

    public String getActionForm(Record record){
        return record.library.getActionForm();
    }

}
