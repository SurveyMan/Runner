package edu.umass.cs.runner.system.backend;

import edu.umass.cs.runner.Record;

public interface ITask {

    public String getTaskId();
    public Record getRecord();
    public void setRecord(Record record);
    @Override
    public boolean equals(Object o);
    @Override
    public int hashCode();

}
