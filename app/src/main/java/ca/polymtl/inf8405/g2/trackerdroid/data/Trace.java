package ca.polymtl.inf8405.g2.trackerdroid.data;


import org.bson.Document;

import java.io.Serializable;


/**
 * Created by marak on 2019-03-22.
 */

public class Trace implements Serializable {
    private int index;
    private double lat;
    private double lng;
    private int speed;

    public Trace(int index, double lat, double lng, int speed) {
        this.index = index;
        this.lat = lat;
        this.lng = lng;
        this.speed = speed;
    }

    public Trace(Document doc) {
        this.index = doc.getInteger("index");
        this.lat = doc.getDouble("lat");
        this.lng = doc.getDouble("lng");
        this.speed = doc.getInteger("speed");
    }

    public int getIndex() {
        return index;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public float getSpeed() {
        return speed;
    }
}
