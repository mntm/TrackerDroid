package ca.polymtl.inf8405.g2.trackerdroid.manager;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.LocationListener;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import ca.polymtl.inf8405.g2.trackerdroid.MainActivity;
import ca.polymtl.inf8405.g2.trackerdroid.R;
import ca.polymtl.inf8405.g2.trackerdroid.controller.GPSController;
import ca.polymtl.inf8405.g2.trackerdroid.controller.StepCounterController;
import ca.polymtl.inf8405.g2.trackerdroid.data.Path;
import ca.polymtl.inf8405.g2.trackerdroid.data.User;
import ca.polymtl.inf8405.g2.trackerdroid.data.db.AuthAndLoginListener;
import ca.polymtl.inf8405.g2.trackerdroid.data.db.DBHelper;
import ca.polymtl.inf8405.g2.trackerdroid.data.db.FindOptions;

/**
 * Created by marak on 2019-03-22.
 */

public class Manager implements Observer {
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    public static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 104;
//    public static final String APP_ROOT_DIRECTORY = Environment.get.getPath().concat(File.separator).concat("TrackerDroid").concat(File.separator);
//    public static final String DATABASE_ROOT_DIRECTORY = APP_ROOT_DIRECTORY.concat("db");

    private static final String TAG = "MGR";

    private static Manager ourInstance = null;

    private DBHelper dbhelper;
    private WeakReference<Context> main;

    private GPSController gps;
    private boolean locationHandled = false;

    private StepCounterController stepCounter;

    private boolean dbInit = false;

    private boolean recording = false;
    private int myRecord = -Integer.MAX_VALUE;
    private int recordToBeat = -Integer.MAX_VALUE;

    private Manager() {
    }

    public boolean isDbHelperNull() {
        if (dbhelper == null)
            return true;
        return false;
    }

    public static Manager BuildInstance() {

        return Manager.getInstance();
    }

    public static Manager getInstance() {
        if (ourInstance == null) {
            ourInstance = new Manager();
        }
        return ourInstance;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public void setContext(Context activity) {
        this.main = new WeakReference<>(activity);
    }

    //{{ DATABASE RELATED
    public void createDBHelperObject(AuthAndLoginListener listener) {
        if (this.dbhelper == null) {
            this.dbhelper = new DBHelper(listener, this.main.get().getString(R.string.stitch_client_app_id));
        }
    }

    public boolean initDatabase() {
        this.dbInit = this.dbhelper.initCollection("mongodb-atlas", "db", "tracker");
        return dbInit;
    }

    public boolean isDatabaseInit() {
        return this.dbInit;
    }
//}}

    public String getFilePath() {
        return this.dbhelper.getFilePath();
    }

    public ArrayList<Path> getPathList() {
        ArrayList<Path> ret = new ArrayList<>();

        Document filter = new Document()
                .append("user_id", this.getUserId())
                .append("type", "path")
                .append("steps", new Document().append("$exists", true));

        Document sortDoc = new Document().append("index", 1);

        FindOptions.Builder builder = new FindOptions.Builder(filter).setSortDocument(sortDoc);

        ArrayList<Document> documents = this.dbhelper.find(builder.build());

        if (documents != null) {
            for (Document document : documents) {
                ret.add(new Path(document));
            }
        }

        return ret;
    }

    //{{ LOCATION RELATED
    public boolean handleLocation() {
        this.gps = GPSController.buildInstance(this.main.get());
        this.gps.addObserver(this);
        locationHandled = this.gps.isEnabled();
        return locationHandled;
    }

    public void startLocationUpdates(int minTime, int minDistance, @Nullable LocationListener listener) {
        if (locationHandled) {
            this.gps.setLocationListener(listener);
            this.gps.startLocationUpdates(minTime, minDistance);
        }
    }

    public void stopLocationUpdates() {
        if (locationHandled)
            this.gps.stopLocationUpdates();
    }
//}}

    public int getCurrentSpeed() {
        return this.gps.getCurrentSpeed();
    }

    public LatLng getCurrentPosition() {
        return this.gps.getCurrentPosition();
    }

    //{{ STEP COUNTER RELATED
    public boolean handleStepCounting() {
        this.stepCounter = StepCounterController.buildInstance(this.main.get());
        if (this.stepCounter == null) return false;
        this.stepCounter.addObserver(this);
        return true;
    }

//}}

    public void startCountingSteps(@Nullable SensorEventListener listener) {
        if (this.stepCounter != null) {
            this.stepCounter.setSensorEventListener(listener);
            this.stepCounter.startCountingSteps();
        }
    }

    public void stopCountingSteps() {
        if (this.stepCounter != null)
            this.stepCounter.stopCountingSteps();
    }

    public void terminate() {
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }

    public String getUserId() {
        return this.dbhelper.getUserId();
    }

    public String setUser(User user) {
        Document name = new Document();
        name.put("first", user.getFname());
        name.put("name", user.getName());

        Document _user = new Document();
        _user.put("name", name);
        _user.put("photo", user.getB64Photo());

        Document doc = new Document();
        doc.put("user_id", this.dbhelper.getUserId());
        doc.put("user", _user);
        doc.put("type", "user");
        doc.put("record", 0);
        doc.put("recordToBeat", 0);
        doc.put("npath", 0);

        BsonValue insert = this.dbhelper.insert(doc);
        return (insert == null) ? null : insert.toString();
    }

    public User getUser() {
        Document doc = new Document();
        doc.put("user_id", this.getUserId());
        doc.put("type", "user");

        Document filter = new Document();
        filter.append("_id", 0);
        filter.append("user", 1);

        FindOptions.Builder builder = new FindOptions.Builder(doc);
        builder.setProjectionDocument(filter).setLimit(1);
        ArrayList<Document> find = this.dbhelper.find(builder.build());

        if (find == null) return null;

        myRecord = this.getMyRecord();
        recordToBeat = this.getRecordToBeat();
        return (find.size() > 0) ? new User(find.get(0)) : null;
    }

    public int getMyRecord() {
        if (myRecord == -Integer.MAX_VALUE) {
            Document findDoc = new Document();
            findDoc.append("user_id", this.getUserId())
                    .append("type", "user");

            Document projectionDoc = new Document("record", 1).append("_id", 0);

            FindOptions.Builder builder = new FindOptions.Builder(findDoc);
            builder.setProjectionDocument(projectionDoc);
            ArrayList<Document> documents = this.dbhelper.find(builder.build());

            if (documents == null || documents.size() == 0) return -Integer.MAX_VALUE;

            return documents.get(0).getInteger("record");
        }
        return myRecord;
    }

    public void setMyRecord(int record) {
        Document filterDoc = new Document();
        filterDoc.append("user_id", this.getUserId()).append("type", "user");

        Document updateDoc = new Document("$set", new Document("record", record));
        this.updateCollection(filterDoc, updateDoc);
        this.myRecord = record;
    }

    public int getRecordToBeat() {
        if (recordToBeat == -Integer.MAX_VALUE) {
            Document findDoc = new Document();
            findDoc.append("user_id", this.getUserId())
                    .append("type", "user");

            Document projectionDoc = new Document("recordToBeat", 1).append("_id", 0);

            FindOptions.Builder builder = new FindOptions.Builder(findDoc);
            builder.setProjectionDocument(projectionDoc);
            ArrayList<Document> documents = this.dbhelper.find(builder.build());

            if (documents == null || documents.size() == 0) return -Integer.MAX_VALUE;

            return documents.get(0).getInteger("recordToBeat");
        }
        return recordToBeat;
    }

    public void setRecordToBeat(int recordToBeat) {
        Document filterDoc = new Document();
        filterDoc.append("user_id", this.getUserId()).append("type", "user");

        Document updateDoc = new Document("$set", new Document("record", recordToBeat));
        this.updateCollection(filterDoc, updateDoc);
        recordToBeat = recordToBeat;
    }

    @Override
    public void update(Observable observable, Object o) {
        if (observable instanceof StepCounterController) {
            if (main.get() instanceof MainActivity)
                ((MainActivity) main.get()).onSensorChanged((SensorEvent) o);
        }
    }

    public long deleteDocument(ObjectId objectId) {
        Document deleteDoc = new Document();
        deleteDoc.append("user_id", this.getUserId()).append("_id", objectId);

        return this.dbhelper.delete(deleteDoc);
    }


    public RemoteUpdateResult updateCollection(Document filterDoc, Document updateDoc) {
        return this.dbhelper.update(filterDoc, updateDoc);
    }

    public ArrayList<Document> findCollection(Document findDoc, Document projectionDoc, int limit) {
        FindOptions.Builder builder = new FindOptions.Builder(findDoc);
        builder.setFindDocument(findDoc).setProjectionDocument(projectionDoc).setLimit(limit);
        return this.dbhelper.find(builder.build());
    }

    public BsonValue insertCollection(Document insertDoc) {
        return this.dbhelper.insert(insertDoc);
    }


    public int getRelativeSteps() {
        if (this.stepCounter != null)
            return this.stepCounter.getRelativeSteps();
        return 0;
    }

    public int getSinceBootSteps() {
        if (this.stepCounter != null)
            return this.stepCounter.getSinceBootSteps();
        return 0;
    }

    public int getOverallSteps() {
        if (this.stepCounter != null)
            return this.stepCounter.getOverallSteps();
        return 0;
    }
}
