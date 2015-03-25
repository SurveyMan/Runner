package edu.umass.cs.runner.system.backend.known.localhost;

import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.backend.ITask;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;

import java.io.IOException;
import java.io.Serializable;

public class LocalTask implements ITask, Serializable {

    private static Gensym gensym = new Gensym("task");

    private String taskid;
    private Record record;

    public LocalTask(Record record) throws IOException, SurveyException {
        this.taskid = gensym.next();
        this.record = record;
        record.addNewTask(this);
    }

    public LocalTask(Record record, String taskid) throws IOException, SurveyException {
        this(record);
        this.taskid = taskid;
    }

    @Override
    public String getTaskId() {
        return taskid;
    }

    @Override
    public Record getRecord() {
        return record;
    }

    @Override
    public void setRecord(Record record) {
        this.record = record;
        this.record.addNewTask(this);
    }

    @Override
    public boolean equals(Object o) {
        return this.taskid.equals(((LocalTask) o).getTaskId())
                && this.record.rid.equals(((LocalTask) o).getRecord().rid);
    }

    @Override
    public int hashCode(){
        return taskid.hashCode() ^ record.rid.hashCode();
    }
}
