package ca.polymtl.inf8405.g2.trackerdroid;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.bson.BsonValue;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Calendar;

import ca.polymtl.inf8405.g2.trackerdroid.data.Path;
import ca.polymtl.inf8405.g2.trackerdroid.data.Trace;
import ca.polymtl.inf8405.g2.trackerdroid.manager.Manager;

// TODO Verifier que les callbacks onLocationChanged et onSensorChanged sont correctement appeles
public class RecordingService extends Service implements LocationListener, SensorEventListener {
    public static final int START = 1;
    public static final int STOP = 0;
    private static final int NOTIFICATION_ID = 620922213;
    private static final String TAG = RecordingService.class.getSimpleName();
    private Messenger messenger;

    private Manager manager = null;
    private Path currentPath;

    public RecordingService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created!");

        if (this.manager == null) {
            this.manager = Manager.getInstance();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        messenger = (Messenger) intent.getExtras().get("messenger");
        Message message = Message.obtain();
        message.obj = new Messenger(new RecordingHandler());
        message.arg1 = 0;
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }

    public void startRecording() {
        createNotification();
        this.manager.startCountingSteps(this);
        this.manager.startLocationUpdates(500, 0, this);

        currentPath = new Path(Calendar.getInstance().getTime());

        // get the number of path
        Document test = new Document();
        test.append("user_id", this.manager.getUserId());

        Document getPath = new Document();
        getPath.append("npath", 1);

        ArrayList<Document> result = this.manager.findCollection(test, getPath, 1);

        currentPath.setIndex(result.get(0).getInteger("npath"));

        // Create a new Path document
        Document insertDoc = new Document()
                .append("user_id", this.manager.getUserId())
                .append("type", "path")
                .append("index", currentPath.getIndex())
                .append("dt_start", currentPath.getStart());

        BsonValue insert = this.manager.insertCollection(insertDoc);

        if (insert != null) {

            currentPath.setObjectId(insert.asObjectId().getValue());

            // increment the number of path
            Document filterDoc = new Document();
            filterDoc.append("user_id", this.manager.getUserId()).append("type", "user");
            Document updateDoc = new Document();
            updateDoc.append("$inc", new Document().append("npath", 1));
            this.manager.updateCollection(filterDoc, updateDoc);
        }

        Log.d(TAG, "Recording started");

    }

    public void stopRecording() {
        this.manager.stopCountingSteps();
        this.manager.stopLocationUpdates();

        if (currentPath == null) return;

        currentPath.addSteps(this.manager.getRelativeSteps());
        currentPath.setEnd(Calendar.getInstance().getTime());

        onLocationChanged(null);

        Document filterDoc = new Document()
                .append("_id", currentPath.getObjectId());

        Document updateDoc = new Document();
        updateDoc.append("$set", new Document()
                .append("steps", currentPath.getTotalSteps())
                .append("dt_end", currentPath.getEnd()));
        this.manager.updateCollection(filterDoc, updateDoc);

        if (currentPath.getTotalSteps() > this.manager.getMyRecord())
            this.manager.setMyRecord(currentPath.getTotalSteps());

        Message m = Message.obtain();
        m.arg1 = 1;
        m.obj = currentPath;
        try {
            messenger.send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        stopForeground(true);

        Log.d(TAG, "Recording stopped");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        currentPath.addSteps(this.manager.getRelativeSteps());
        Log.d(TAG, String.format("Step count: %d", currentPath.getTotalSteps()));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        int index = currentPath.getTraces().size();

        LatLng coord = this.manager.getCurrentPosition();

        int speed = this.manager.getCurrentSpeed();

        currentPath.addTrace(new Trace(index, coord.latitude, coord.longitude, speed));

        Document traceDoc = new Document();
        traceDoc.append("index", index)
                .append("lat", coord.latitude)
                .append("lng", coord.longitude)
                .append("speed", speed);

        Document filterDoc = new Document()
                .append("_id", currentPath.getObjectId());

        Document updateDoc = new Document()
                .append("$push", new Document().append("traces", traceDoc));

        this.manager.updateCollection(filterDoc, updateDoc);
        Log.d(TAG, String.format("Current Location: %s;\tSpeed: %dm/s", coord.toString(), speed));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


    private void createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, getString(R.string.app_name));
        builder.setSmallIcon(R.drawable.ic_directions_walk)
                .setContentIntent(pendingIntent)
                .setContentTitle(getText(R.string.app_name))
                .setContentText("")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null);

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    public class RecordingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int state = msg.arg1;
            switch (state) {
                case START:
                    startRecording();
                    break;
                case STOP:
                    stopRecording();
                    break;
            }
        }
    }
}
