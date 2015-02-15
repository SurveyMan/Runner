package edu.umass.cs.runner.system.backend;

import edu.umass.cs.runner.Record;

public interface IHTML {

    public String getHTMLString();
    public String getActionForm(Record record);

}
