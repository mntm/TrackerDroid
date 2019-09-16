package ca.polymtl.inf8405.g2.trackerdroid;


import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorEvent;
import android.net.TrafficStats;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.directions.route.AbstractRouting;
import java.util.Locale;
import ca.polymtl.inf8405.g2.trackerdroid.data.Path;
import ca.polymtl.inf8405.g2.trackerdroid.data.User;
import ca.polymtl.inf8405.g2.trackerdroid.data.db.DBHelper;
import ca.polymtl.inf8405.g2.trackerdroid.manager.Manager;
import ca.polymtl.inf8405.g2.trackerdroid.utils.PermissionUtils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ActivityCompat.OnRequestPermissionsResultCallback, PathAdapter.ActionListener, NfcAdapter.CreateNdefMessageCallback {

    private static final String TAG = "MAIN";

    private Messenger messenger;
    private TextView counterView;
    private TextView recordToBeatView;
    private TextView recordView;
    private PathAdapter adapter = new PathAdapter(this);
    private RecyclerView pathList;
    private Intent serviceIntent;
    private NfcAdapter nfcAdapter;

    TextView status;
    TextView level;
    TextView health;
    TextView rx;
    TextView tx;
    IntentFilter inf;
    private long lastTotalRxBytes;
    private long lastTotalTxBytes;
    private long lastRxTimeStamp;
    private long lastTxTimeStamp;
    private BroadcastReceiver br;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Manager.getInstance().isDbHelperNull()) {
            startActivity(new Intent(MainActivity.this, SplashScreen.class));
        }

        Manager.getInstance().setContext(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        User user = (User) getIntent().getSerializableExtra("user");
        if ((user == null) && ((user = Manager.getInstance().getUser()) == null)) {
            startActivity(new Intent(MainActivity.this, SplashScreen.class));
        }


        ImageView photo = navigationView.findViewById(R.id.imageView);
        photo.setImageBitmap(user.getPhoto());

        TextView name = navigationView.findViewById(R.id.txt_name);
        String _name = user.getFname() + " " + user.getName();
        name.setText(_name);

        pathList = findViewById(R.id.recycler_view);
        pathList.setAdapter(this.adapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        pathList.setLayoutManager(layoutManager);
        for (Path p : Manager.getInstance().getPathList()) {
            adapter.addItem(p);
        }

        counterView = findViewById(R.id.stepCount);
        recordView = findViewById(R.id.record);
        recordToBeatView = findViewById(R.id.recordToBeat);

        recordView.setText(getString(R.string.default_record_message) + " " + String.valueOf(Manager.getInstance().getMyRecord()));
        recordToBeatView.setText(getString(R.string.default_record_to_beat_message) + " " + String.valueOf(Manager.getInstance().getRecordToBeat()));

        if (!Manager.getInstance().handleLocation()) {
            PermissionUtils.requestPermission(this, Manager.LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }

        attachNfcEvent();

        setUI();
        inf = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int stat = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                Boolean isCharging = stat == BatteryManager.BATTERY_STATUS_CHARGING || stat == BatteryManager.BATTERY_STATUS_FULL;
                if (isCharging) {
                    status.setText("Charging");
                } else {
                    status.setText("Discharging");
                }

                int helth = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                switch (helth) {
                    case BatteryManager.BATTERY_HEALTH_COLD:
                        health.setText("Battery COLD");
                        break;
                    case BatteryManager.BATTERY_HEALTH_DEAD:
                        health.setText("Battery DEAD");
                        break;
                    case BatteryManager.BATTERY_HEALTH_GOOD:
                        health.setText("Battery GOOD");
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                        health.setText("Battery OVER VOLTAGE");
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                        health.setText("Battery OVERHEAT");
                        break;
                    case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                        health.setText("Battery HEALTH UNKNOWN");
                        break;
                }
                int lev = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float battery = (lev / (float) scale) * 100;
                level.setText(String.valueOf(
                        battery
                ));


            }
        };
        MainActivity.this.registerReceiver(br, inf);

        rx.setText(getRXNetSpeed(this));
        tx.setText(getTXNetSpeed(this));
        handleNfcIntent(getIntent());
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if ((requestCode == Manager.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) &&
                (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.WRITE_EXTERNAL_STORAGE)) &&
                (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.READ_EXTERNAL_STORAGE))) {
            Log.d(TAG, "Storage permissions granted!");
        }

        if ((requestCode == Manager.LOCATION_PERMISSION_REQUEST_CODE) && (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION))) {
            Log.d(TAG, "Location permissions granted!");
            // Enable the my location layer if the permission has been granted.
            if (!Manager.getInstance().handleLocation()) {
                new AlertDialog.Builder(this)
                        .setMessage("Location rights has been permited, but it is still deactivated.\n" +
                                "Remember to activate it later.")
                        .setCancelable(true).create().show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Manager.getInstance().isDbHelperNull()) {
            startActivity(new Intent(MainActivity.this, SplashScreen.class));
        }

        handleNfcIntent(getIntent());

        serviceIntent = new Intent(MainActivity.this, RecordingService.class);
        serviceIntent.putExtra("messenger", new Messenger(new MyHandler()));
        startService(serviceIntent);

        FloatingActionButton fab = findViewById(R.id.fab);
        if (Manager.getInstance().isRecording()) {
            fab.setImageResource(R.drawable.ic_save_black_24dp);
        } else {
            fab.setImageResource(R.drawable.ic_directions_walk);
        }
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        String STEP_COUNTER_PHRASE = "Your current step count since recording started is : %d\n";
        counterView.setText(String.format(Locale.ENGLISH, STEP_COUNTER_PHRASE, Manager.getInstance().getOverallSteps()));
        String message_update = getString(R.string.default_record_message) + " " + String.valueOf(Manager.getInstance().getMyRecord());
        recordView.setText(message_update);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.this.unregisterReceiver(br);
    }

    public void toggleRecording(View view) throws RemoteException {
        FloatingActionButton fab = (FloatingActionButton) view;
        if (messenger == null) return;
        if (!Manager.getInstance().isRecording()) {
            if (Manager.getInstance().getCurrentPosition() == null) {
                Toast.makeText(this, "Position not known yet!", Toast.LENGTH_LONG).show();
            }

            if (!Manager.getInstance().handleStepCounting()) {
                Snackbar.make(view, "Your device do not support step counter sensor.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
            counterView.setText(getString(R.string.default_stepcounter_message));
            Message message = Message.obtain();
            message.arg1 = RecordingService.START;
            messenger.send(message);
            fab.setImageResource(R.drawable.ic_save_black_24dp);
            Manager.getInstance().setRecording(true);
        } else {
            Message message = Message.obtain();
            message.arg1 = RecordingService.STOP;
            messenger.send(message);
            fab.setImageResource(R.drawable.ic_directions_walk);
            Manager.getInstance().setRecording(false);
            String message_update = getString(R.string.default_record_to_beat_message) +
                    " " +
                    String.valueOf(Manager.getInstance().getRecordToBeat());
            recordToBeatView.setText(message_update);
            counterView.setText(getString(R.string.default_stepcounter_message));
            message_update = getString(R.string.default_record_message) + " " + String.valueOf(Manager.getInstance().getMyRecord());
            recordView.setText(message_update);
        }
    }

    @Override
    public void onClick(View view) {
        MapsDialogFragment map = new MapsDialogFragment();


        int position = pathList.getChildAdapterPosition(view);
        Path itemAt = adapter.getItemAt(position);


        Bundle args = new Bundle();
        args.putSerializable(getString(R.string.waypoints_arg_name), itemAt);
        args.putSerializable(getString(R.string.travelmode_arg_name), AbstractRouting.TravelMode.WALKING);

        map.setArguments(args);
        map.show(getSupportFragmentManager(), TAG);
    }

    @Override
    public boolean onLongClick(View view) {

        int position = pathList.getChildAdapterPosition(view);
        Path itemAt = adapter.getItemAt(position);

        AlertDialog dialog = new AlertDialog.Builder(this).setCancelable(true)
                .setMessage(R.string.confirm_deletion).create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialogInterface, i) -> {
            if (Manager.getInstance().deleteDocument(itemAt.getObjectId()) > 0) {
                adapter.removeItem(itemAt);
            }
            dialog.dismiss();
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", (dialogInterface, i) -> dialog.dismiss());

        dialog.show();

        return true;
    }

    public void attachNfcEvent() {
        Log.d("Attach nfc", "Starting attachment");
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter != null)
            nfcAdapter.setNdefPushMessageCallback(this, this);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        String message = String.valueOf(Manager.getInstance().getMyRecord());
        NdefRecord ndefRecord = NdefRecord.createMime("text/plain", message.getBytes());
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage;
    }

    private void handleNfcIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage message = (NdefMessage) rawMessages[0];
            String recordToBeat = new String(message.getRecords()[0].getPayload());
            Manager.getInstance().setRecordToBeat(Integer.parseInt(recordToBeat));
            String message_update = getString(R.string.default_record_to_beat_message) + " " + recordToBeat;
            recordToBeatView.setText(message_update);
            Log.d("NFC Record Update", "New record to beat is : " + recordToBeat);
        }
    }

    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int arg = msg.arg1;
            switch (arg) {
                case 0:
                    messenger = (Messenger) msg.obj;
                    Log.d(TAG, "Messenger set");
                    break;
                case 1:
                    adapter.addItem((Path) msg.obj);
                    Log.d(TAG, "Path added to adapter");
                    break;
            }

        }
    }

    public void setUI() {
        status = findViewById(R.id.Status);
        level = findViewById(R.id.Level);
        health = findViewById(R.id.Hl);
        rx = findViewById(R.id.Rx);
        tx = findViewById(R.id.Tx);
    }
// Source: https://www.programcreek.com/java-api-examples/?api=android.net.TrafficStats
    public String getRXNetSpeed(Context context) {

        String netSpeed = "0 kb/s";
        long nowTotalRxBytes = TrafficStats.getUidRxBytes(context.getApplicationInfo().uid) ==
                TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);

        long nowTimeStamp = System.currentTimeMillis();

        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / (nowTimeStamp - lastRxTimeStamp));

        lastRxTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        netSpeed = String.valueOf(speed) + " kb/s";

        return netSpeed;
    }
// Source: https://www.programcreek.com/java-api-examples/?api=android.net.TrafficStats
    public String getTXNetSpeed(Context context) {

        String netSpeed = "0 kb/s";
        long nowTotalTxBytes = TrafficStats.getUidTxBytes(context.getApplicationInfo().uid) ==
                TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalTxBytes() / 1024);

        long nowTimeStamp = System.currentTimeMillis();

        long speed = ((nowTotalTxBytes - lastTotalTxBytes) * 1000 / (nowTimeStamp - lastTxTimeStamp));

        lastTxTimeStamp = nowTimeStamp;
        lastTotalTxBytes = nowTotalTxBytes;
        netSpeed = String.valueOf(speed) + " kb/s";

        return netSpeed;
    }
}
