package ca.polymtl.inf8405.g2.trackerdroid.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.bson.Document;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

/**
 * Created by marak on 2019-03-22.
 */

public class User implements Serializable {
    private String name;
    private String fname;
    private String b64_photo;

    public User(String name, String fname) {
        this.name = name;
        this.fname = fname;
    }

    public User(final Document doc) {
        Document user = doc.get("user", Document.class);
        Document name = user.get("name", Document.class);

        this.name = name.getString("name");
        this.fname = name.getString("first");
        this.setB64Photo(user.getString("photo"));
    }


    public Bitmap getPhoto() {
        byte[] decode = Base64.decode(
                this.b64_photo.substring(this.b64_photo.indexOf(",") + 1),
                Base64.DEFAULT
        );
        return BitmapFactory.decodeByteArray(decode, 0, decode.length);
    }

    public void setPhoto(Bitmap photo) {
        Bitmap ph = photo;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ph.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        this.b64_photo = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    public String getName() {
        return name;
    }

    public String getFname() {
        return fname;
    }

    public String getB64Photo() {
        return b64_photo;
    }

    private void setB64Photo(String b64_photo) {
        this.b64_photo = b64_photo;
    }
}
