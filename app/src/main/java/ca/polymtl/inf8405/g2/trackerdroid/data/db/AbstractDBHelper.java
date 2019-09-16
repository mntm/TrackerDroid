package ca.polymtl.inf8405.g2.trackerdroid.data.db;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.core.StitchAppClientConfiguration;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by marak on 2019-03-26.
 */

public abstract class AbstractDBHelper {
    protected static final String TAG = "DBH";

    protected StitchAppClient mAppClient = null;
    protected String mUid;

    public AbstractDBHelper(AuthAndLoginListener authListener) {
        init(authListener);
    }

    public AbstractDBHelper(AuthAndLoginListener authListener, String appid, String rootDirectory) {
//        if (!Stitch.hasAppClient(appid)){
        Log.d(TAG, "Init: AppClient - No default client");
        StitchAppClientConfiguration.Builder confBuilder = new StitchAppClientConfiguration.Builder();
        confBuilder.withDataDirectory(rootDirectory);

        mAppClient = Stitch.initializeDefaultAppClient(appid, confBuilder.build());
//        }
//        else{
//            Log.d(TAG,"Init: AppClient - Have default client");
//        }

        init(authListener);
    }

    private void init(AuthAndLoginListener authListener) {

        Log.d(TAG, "Init: Auth MongoDB");
        if (mAppClient == null)
            mAppClient = Stitch.getDefaultAppClient();

        mAppClient.getAuth().addAuthListener(authListener);
        mAppClient.getAuth().loginWithCredential(new AnonymousCredential()).addOnCompleteListener(
                new OnCompleteListener<StitchUser>() {
                    @Override
                    public void onComplete(@NonNull Task<StitchUser> task) {
                        if (task.isSuccessful()) {
                            mUid = task.getResult().getId();
                            Log.i("stitch", mUid);
                        } else {
                            Log.e("stitch", "failed to log in anonymously", task.getException());
                        }
                        authListener.onComplete(task);
                    }
                }
        );
    }

    public void close() {

        if (mAppClient.getAuth().isLoggedIn()) {
            mAppClient.getAuth().logout().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    try {
                        mAppClient.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            try {
                mAppClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getUserId() {
        return this.mUid;
    }

    public abstract BsonValue insert(Document doc);

    public abstract RemoteUpdateResult update(Bson filter, Bson value);

    public abstract ArrayList<Document> find(FindOptions findOptions);

    public abstract long delete(Bson filter);


}
