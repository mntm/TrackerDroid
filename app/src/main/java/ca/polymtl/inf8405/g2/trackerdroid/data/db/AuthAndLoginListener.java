package ca.polymtl.inf8405.g2.trackerdroid.data.db;

import com.google.android.gms.tasks.OnCompleteListener;
import com.mongodb.stitch.android.core.auth.StitchAuthListener;
import com.mongodb.stitch.android.core.auth.StitchUser;

/**
 * Created by marak on 2019-04-03.
 */

public interface AuthAndLoginListener extends StitchAuthListener, OnCompleteListener<StitchUser> {
}
