package unipi.protal.smartgreecealert.entities;

import android.graphics.Bitmap;

import java.io.Serializable;

public class FireReport implements Serializable {
    private String latitude ;
    private String longitude;
    private String date;
    private String photo;
    private String canceled;

    public FireReport(String latitude, String longitude, String date, String photo, String canceled) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.photo = photo;
        this.canceled = canceled;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getCanceled() {
        return canceled;
    }

    public void setCanceled(String canceled) {
        this.canceled = canceled;
    }
}
