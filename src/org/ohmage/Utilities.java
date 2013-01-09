/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.ohmage;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

public class Utilities {
    private static final String TAG = "Utilities";

    public static class KVPair {

        public String key;
        public String value;

        public KVPair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class KVLTriplet {

        public String key;
        public String value;
        public String label;

        public KVLTriplet(String key, String value, String label) {
            this.key = key;
            this.value = value;
            this.label = label;
        }
    }

    public static void delete(File f) throws IOException {
        if (f == null)
            return;
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        f.delete();
    }

    public static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the
         * BufferedReader.readLine() method. We iterate until the BufferedReader
         * return null which means there's no more data to read. Each line will
         * appended to a StringBuilder and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * Converts an array of numbers to an array of doubles that can be used by
     * the histogram and sparkline charts
     * 
     * @param data
     * @return
     */
    public static double[] toArray(List<? extends Number> data) {
        double[] ret = new double[data.size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = data.get(i).doubleValue();
        return ret;
    }

    public static BitmapFactory.Options decodeImageOptions(File f) throws IOException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        FileInputStream fis = new FileInputStream(f);
        BitmapFactory.decodeStream(fis, null, o);
        fis.close();

        return o;
    }

    /**
     * Decodes an image file in a memory efficient way
     * 
     * @param f file to decode
     * @param maxSize length of the longer side
     * @return the bitmap object that has be resized
     * @throws IOException
     */
    public static Bitmap decodeImage(File f, int maxSize) throws IOException {
        return decodeImage(f, decodeImageOptions(f), maxSize);
    }

    /**
     * Decodes an image file in a memory efficient way. use
     * {@link #decodeImage(File, int)} unless you already called
     * {@link #decodeImageOptions(File)} and have the image options avaliable.
     * 
     * @param f file to decode
     * @param o options file from #{@link #decodeImageOptions(File)}
     * @param maxSize length of the longer side
     * @return the bitmap object that has be resized
     * @throws IOException
     */
    public static Bitmap decodeImage(File f, Options o, int maxSize) throws IOException {
        Bitmap b = null;
        int scale = 1;
        if (o.outHeight > maxSize || o.outWidth > maxSize) {
            scale = (int) Math.pow(
                    2,
                    (int) Math.round(Math.log(maxSize / (double) Math.max(o.outHeight, o.outWidth))
                            / Math.log(0.5)));
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        FileInputStream fis = new FileInputStream(f);
        b = BitmapFactory.decodeStream(fis, null, o2);
        fis.close();
        return b;
    }

    /**
     * Resize the image at the file and save it back
     * 
     * @param f
     * @param maxSize
     * @return true if the image was saved successfully
     */
    public static boolean resizeImageFile(File f, int maxSize) {
        Bitmap b = null;
        try {
            Options o = decodeImageOptions(f);

            // If its already smaller, don't do anything
            if (o.outHeight < maxSize && o.outWidth < maxSize)
                return true;

            b = decodeImage(f, o, maxSize);
            if (b != null) {
                try {
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(f);
                        b.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    } finally {
                        if (out != null) {
                            out.close();
                            return true;
                        }
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Image resize failed.", e);
        } finally {
            if (b != null)
                b.recycle();
        }
        return false;
    }

    /**
     * Counts the numnber of bytes read in the inputstream
     * 
     * @author cketcham
     */
    public static class CountingInputStream extends FilterInputStream {

        private long mLength;

        public CountingInputStream(InputStream is) {
            super(is);
            mLength = 0;
        }

        @Override
        public int read() throws IOException {
            mLength++;
            return super.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
            int c = super.read(buffer, offset, count);
            mLength += c;
            return c;
        }

        public long amountRead() {
            return mLength;
        }
    }

    /**
     * Darkens the color value
     * 
     * @param color
     * @return
     */
    public static int darkenColor(int color) {
        return changeColor(color, -60);
    }

    /**
     * Lightens color value
     * 
     * @param color
     * @return
     */
    public static int lightenColor(int color) {
        return changeColor(color, 15);
    }

    /**
     * Change the color by the specified amount
     * 
     * @param color
     * @param amount
     * @return
     */
    private static int changeColor(int color, int amount) {
        amount = fitToColor(Color.red(color), amount);
        amount = fitToColor(Color.green(color), amount);
        amount = fitToColor(Color.blue(color), amount);
        return Color.rgb(Color.red(color) + amount, Color.green(color) + amount, Color.blue(color)
                + amount);
    }

    /**
     * Makes sure that an adjustment by this amount for this part of the color
     * wont overflow (which will cause the overall color to change)
     * 
     * @param color
     * @param change
     * @return
     */
    private static int fitToColor(int color, int change) {
        if (color + change < 0 || color + change > 255)
            return 255 - color;
        return change;
    }

    /**
     * Calculates the grayscale value of the color
     * 
     * @param color
     * @return the gray scale color
     */
    public static int colorGrayscale(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = 0;
        hsv[1] = 0;
        return Color.HSVToColor(hsv);
    }

    /**
     * utility method for converting dp to pixels, since the setters only take
     * pixel values :\
     * 
     * @param dp value
     * @return
     */
    public static int dpToPixels(int dp) {
        final float scale = OhmageApplication.getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * Clears the time component of a calendar object
     * 
     * @param cal
     */
    public static void clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    public static File fileForMediaStore(Uri uri) {
        String[] filePathColumn = {
            MediaStore.Images.Media.DATA
        };

        Cursor cursor = OhmageApplication.getContext().getContentResolver()
                .query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String filePath = cursor.getString(columnIndex);
        cursor.close();
        return new File(filePath);
    }

    public static int moveMediaStoreFile(Uri uri, File file) {
        if (moveFile(fileForMediaStore(uri), file))
            return OhmageApplication.getContext().getContentResolver().delete(uri, null, null);
        return -1;
    }

    public static boolean moveFile(File src, File dest) {
        if (!src.renameTo(dest)) {
            try {
                copy(src, dest);
                return src.delete();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Formats an object that might be null to be displayed
     * 
     * @param disp
     * @return ? if it is null, the object otherwise
     */
    public static String getHtmlSafeDisplayString(Object disp) {
        return (disp == null) ? "?" : disp.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *            be executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

}
