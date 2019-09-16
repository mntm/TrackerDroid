package ca.polymtl.inf8405.g2.trackerdroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import ca.polymtl.inf8405.g2.trackerdroid.data.User;
import ca.polymtl.inf8405.g2.trackerdroid.manager.Manager;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "REG";
    private Bitmap mBitmap = null;
    private ImageView regPhoto;
    private TextView name;
    private TextView fname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Manager.getInstance().setContext(this);

        regPhoto = findViewById(R.id.reg_photo);
        name = findViewById(R.id.reg_name);
        fname = findViewById(R.id.reg_first);

        LinearLayout ll = findViewById(R.id.reg_layout_1);
        ll.setElevation(0f);
        ll = findViewById(R.id.reg_layout_2);
        ll.setElevation(0f);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAndRemoveTask();
    }

    public void takePhoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 0);
    }

    public void saveProfile(View view) {
        User user = new User(name.getText().toString(), fname.getText().toString());
        if (mBitmap == null) {
            mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
        }
        user.setPhoto(mBitmap);
        String oid = Manager.getInstance().setUser(user);
        if (oid == null) {
            Toast.makeText(RegisterActivity.this, "Failed to register user!", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, oid);
            Intent i = new Intent(RegisterActivity.this,
                    MainActivity.class);
            i.putExtra("user", user);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            this.mBitmap = (Bitmap) data.getExtras().get("data");
            this.regPhoto.setImageBitmap(mBitmap);
        }
    }
}
