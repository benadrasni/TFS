package au.com.tfsltd.invertebrateKey;

/**
 * Entity which represents single observation
 *
 * Created by adrian on 28.9.2016.
 */

public class Observation {

    private double longitude;
    private double latitude;
    private String photoPath;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}
