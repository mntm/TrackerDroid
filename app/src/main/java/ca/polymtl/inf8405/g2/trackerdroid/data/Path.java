package ca.polymtl.inf8405.g2.trackerdroid.data;

import android.support.annotation.NonNull;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by marak on 2019-03-22.
 */

public class Path implements Serializable, Comparable<Path> {
    private ArrayList<Trace> traces = new ArrayList<>();
    private int total_steps = 0;
    private Date start;
    private Date end;
    private int index;
    private ObjectId objectId;


    public Path(Date start) {
        this.start = start;
    }

    public Path(Document doc) {
        index = doc.getInteger("index");
        start = doc.getDate("dt_start");
        end = doc.getDate("dt_end");
        total_steps = doc.getInteger("steps");
        objectId = doc.getObjectId("_id");

        List<Document> _traces = (List<Document>) doc.get("traces");
        if (_traces == null) return;
        for (Document trace : _traces) {
            this.traces.add(new Trace(trace));
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void addTrace(Trace trace) {
        traces.add(trace);
    }

    public void addSteps(int steps) {
        this.total_steps += steps;
    }

    public ArrayList<Trace> getTraces() {
        return traces;
    }

    public int getTotalSteps() {
        return total_steps;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    @Override
    public int compareTo(@NonNull Path path) {
        return Integer.compare(this.index, path.getIndex());
    }
}
