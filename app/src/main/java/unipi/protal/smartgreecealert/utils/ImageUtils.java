package unipi.protal.smartgreecealert.utils;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

public class ImageUtils {
    public static byte[] encodeBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();
        return data;
    }
}
