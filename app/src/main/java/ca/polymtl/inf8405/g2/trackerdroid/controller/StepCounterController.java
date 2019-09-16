package ca.polymtl.inf8405.g2.trackerdroid.controller;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.lang.ref.WeakReference;
import java.util.Observable;

public class StepCounterController extends Observable implements SensorEventListener {
    private static StepCounterController _inst = null;
    private WeakReference<Context> ctx;
    private SensorManager sm;
    private SensorEventListener listener = null;

    /**
     * Number of steps since boot
     */
    private int sinceBootSteps = 0;
    /**
     * Number of steps since we are listening
     */
    private int overallSteps = 0;
    /**
     * Number of steps since the last event|get
     */
    private int relativeSteps = 0;
    private Sensor sensor;
    private boolean counting = false;

    private StepCounterController() {
    }

    public static StepCounterController buildInstance(Context ctx) {
        if (_inst == null) {
            _inst = new StepCounterController();
            _inst.setContext(ctx);
            _inst.setSensorManager();
            if (!_inst.init()) {
                _inst = null;
            }
        }
        return _inst;
    }

    public static StepCounterController getInstance() {
        return _inst;
    }

    private boolean init() {
        if (this.sm == null) return false;
        sensor = this.sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        return (sensor != null);
    }

    private void setSensorManager() {
        this.sm = (SensorManager) ctx.get().getSystemService(Context.SENSOR_SERVICE);
    }

    public void setContext(Context ctx) {
        this.ctx = new WeakReference<>(ctx);
    }

    public void startCountingSteps() {
        if (!counting)
            this.sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stopCountingSteps() {
        if (counting)
            this.sm.unregisterListener(this);
        this.relativeSteps = 0;
        this.overallSteps = 0;
    }

    public int getSinceBootSteps() {
        return sinceBootSteps + overallSteps;
    }

    public int getOverallSteps() {
        return overallSteps;
    }

    public int getRelativeSteps() {
        int ret = relativeSteps;
        relativeSteps = 0;
        return ret;
    }

    public void setSensorEventListener(SensorEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sinceBootSteps == 0) {
            sinceBootSteps = (int) sensorEvent.values[0];
        }

        relativeSteps = (int) (sensorEvent.values[0] - sinceBootSteps - overallSteps);

        overallSteps = (int) (sensorEvent.values[0] - sinceBootSteps);

        this.setChanged();
        this.notifyObservers(sensorEvent);
        if (listener != null) listener.onSensorChanged(sensorEvent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        if (listener != null) listener.onAccuracyChanged(sensor, i);
    }
}
