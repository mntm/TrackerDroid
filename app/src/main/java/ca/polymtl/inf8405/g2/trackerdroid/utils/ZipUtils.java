package ca.polymtl.inf8405.g2.trackerdroid.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class ZipUtils {
    private static final String TAG = ZipUtils.class.getSimpleName();
    private static final int BUFFER = 2048;

    /**
     * @param files
     * @param zipFile
     * @throws IOException
     * @see "http://web.archive.org/web/20170223112719/http://jondev.net/articles/Zipping_Files_with_Android_(Programmatically)"
     */
    public static void zip(String[] files, String zipFile) throws IOException {

        BufferedInputStream origin;
        FileOutputStream dest = new FileOutputStream(zipFile);

        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

        byte data[] = new byte[BUFFER];

        for (String file : files) {
            Log.d(TAG, "Compress - Adding: " + file);
            FileInputStream fi = new FileInputStream(file);
            origin = new BufferedInputStream(fi, BUFFER);
            ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
            origin.close();
        }

        out.finish();
        out.close();

    }

    /**
     * @param zipFile
     * @param destDir
     * @throws IOException
     * @see "http://web.archive.org/web/20170223112719/http://jondev.net/articles/Unzipping_Files_with_Android_(Programmatically)"
     */
    public static void unzip(String zipFile, String destDir) throws IOException {
        File mDestDir = new File(destDir);
        if (!mDestDir.exists() || !mDestDir.isDirectory()) {
            mDestDir.mkdirs();
        }

        FileInputStream inputStream = new FileInputStream(zipFile);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            Log.d(TAG, "Decompress - Unzipping " + zipEntry.getName());

            if (zipEntry.isDirectory()) {
                File tempDir = new File(mDestDir, zipEntry.getName());
                if (!tempDir.exists() || !tempDir.isDirectory()) {
                    tempDir.mkdirs();
                }
            } else {
                FileOutputStream fout = new FileOutputStream(new File(mDestDir, zipEntry.getName()));
                for (int c = zipInputStream.read(); c != -1; c = zipInputStream.read()) {
                    fout.write(c);
                }

                zipInputStream.closeEntry();
                fout.close();
            }

        }
        zipInputStream.close();
    }
}
