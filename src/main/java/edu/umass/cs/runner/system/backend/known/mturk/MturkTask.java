package edu.umass.cs.runner.system.backend.known.mturk;

import com.amazonaws.mturk.requester.HIT;
import edu.umass.cs.runner.Record;
import edu.umass.cs.runner.system.backend.ITask;

public class MturkTask implements ITask {

    public final HIT hit;
    private Record record;

    MturkTask(HIT hit, Record record) {
        this.hit = hit;
        this.record = record;
        record.addNewTask(this);
    }

    MturkTask(HIT hit){
        this.hit = hit;
    }

    public String getTaskId(){
        return hit.getHITId();
    }

    @Override
    public Record getRecord() {
        return record;
    }

    public void setRecord(Record r) {
        this.record = r;
        this.record.addNewTask(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MturkTask &&
                hit.getHITId().equals(((MturkTask) o).getTaskId()) &&
                record.rid.equals(((MturkTask) o).getRecord().rid);
    }

    @Override
    public int hashCode() {
        return hit.getHITId().hashCode() ^ record.rid.hashCode();
    }
}
