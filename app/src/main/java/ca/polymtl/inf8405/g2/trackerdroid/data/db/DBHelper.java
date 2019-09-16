package ca.polymtl.inf8405.g2.trackerdroid.data.db;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.android.services.mongodb.remote.SyncFindIterable;
import com.mongodb.stitch.core.services.mongodb.remote.ChangeEvent;
import com.mongodb.stitch.core.services.mongodb.remote.ExceptionListener;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;
import com.mongodb.stitch.core.services.mongodb.remote.sync.ChangeEventListener;
import com.mongodb.stitch.core.services.mongodb.remote.sync.DefaultSyncConflictResolvers;
import com.mongodb.stitch.core.services.mongodb.remote.sync.SyncDeleteResult;
import com.mongodb.stitch.core.services.mongodb.remote.sync.SyncInsertOneResult;
import com.mongodb.stitch.core.services.mongodb.remote.sync.SyncUpdateResult;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ca.polymtl.inf8405.g2.trackerdroid.manager.Manager;


/**
 * Created by marak on 2019-03-22.
 */
// TODO changer le repertoire par defaut
public class DBHelper extends AbstractDBHelper {

    private RemoteMongoCollection<Document> mCollection;
    private String filePath = "";
    private String appId;

    public DBHelper(AuthAndLoginListener authListener, String appId) {
        super(authListener);
        this.appId = appId;
    }

    public DBHelper(AuthAndLoginListener authListener, String appId, String rootDirectory) {
        super(authListener, appId, rootDirectory);
        this.appId = appId;
        this.filePath = rootDirectory;
    }

    public boolean initCollection(String serviceName, String dbName, String colName) {
        Log.d(TAG, "Init Collection");
        RemoteMongoClient mMongoClient = mAppClient.getServiceClient(RemoteMongoClient.factory, serviceName);
        mCollection = mMongoClient.getDatabase(dbName).getCollection(colName);
        mCollection.sync().configure(DefaultSyncConflictResolvers.remoteWins(),
                new MyDBUpdateListener(), new MyDBErrorListener());

        StringBuilder sb = new StringBuilder(appId);
        sb.append("sync_mongodb_").append("mongodb-atlas").append(File.separator)
                .append(Manager.getInstance().getUserId()).append(File.separator).append(0)
                .append(File.separator).append("mobile.sqlite");
        filePath = filePath.concat(File.separator).concat(sb.toString());
        return (mCollection != null);
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public BsonValue insert(final Document doc) {
        AtomicReference<BsonValue> ret = new AtomicReference<>();

        Thread runner = new Thread(new Runnable() {
            @Override
            public void run() {
                Task<SyncInsertOneResult> res = mCollection.sync().insertOne(doc);

                SyncInsertOneResult await = null;
                try {
                    await = Tasks.await(res);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (await != null) {
                    Log.d(TAG, await.getInsertedId().toString());
                    ret.set(await.getInsertedId());
                } else {
                    ret.set(null);
                }
            }
        });

        runner.start();

        try {
            runner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ret.get();
    }

    @Override
    public RemoteUpdateResult update(Bson filter, Bson value) {
        AtomicReference<RemoteUpdateResult> ret = new AtomicReference<>();

        Thread runner = new Thread(new Runnable() {
            @Override
            public void run() {
                SyncUpdateResult await = null;
                try {
                    await = Tasks.await(mCollection.sync().updateOne(filter, value));
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ret.set(await);
            }
        });

        runner.start();

        try {
            runner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ret.get();
    }

    @Override
    public ArrayList<Document> find(final FindOptions findOptions) {
        AtomicReference<ArrayList<Document>> set = new AtomicReference<>();

        Thread runner = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Document> docs = new ArrayList<>();
                SyncFindIterable<Document> request = mCollection.sync().find(findOptions.getFindDocument());

                if (findOptions.getSortDocument() != null)
                    request.sort(findOptions.getSortDocument());
                if (findOptions.getFilterDocument() != null)
                    request.filter(findOptions.getFilterDocument());
                if (findOptions.getLimit() == 0)
                    request.limit(findOptions.getLimit());
                if (findOptions.getProjectionDocument() != null)
                    request.projection(findOptions.getProjectionDocument());

                try {
                    Tasks.await(request.forEach(docs::add));
                    set.set(docs);
                    Log.d(TAG, "Find runner thread");
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        runner.start();

        try {
            runner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return set.get();
    }

    @Override
    public long delete(Bson filter) {
        AtomicLong ret = new AtomicLong();

        Thread runner = new Thread(new Runnable() {
            @Override
            public void run() {
                SyncDeleteResult await = null;
                try {
                    await = Tasks.await(mCollection.sync().deleteOne(filter));
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ret.set((await == null) ? -1L : await.getDeletedCount());
            }
        });

        runner.start();

        try {
            runner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret.get();
    }

    private class MyDBUpdateListener implements ChangeEventListener<Document> {
        @Override
        public void onEvent(BsonValue documentId, ChangeEvent<Document> event) {

        }
    }

    private class MyDBErrorListener implements ExceptionListener {
        @Override
        public void onError(BsonValue documentId, Exception error) {

        }
    }
}
