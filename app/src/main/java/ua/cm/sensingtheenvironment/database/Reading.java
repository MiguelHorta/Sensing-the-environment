package ua.cm.sensingtheenvironment.database;

import com.orm.SugarRecord;

/**
 * Created by horta on 30-10-2016.
 */

/***
 * Implemented the readings as easily as possibly.
 * It was attempted to find a standard or recommendation but nothing come up.
 * We settled for the minimum functionality.
 */
public class Reading extends SugarRecord {

    long time;
    double localizationLatitude; // place
    double localizationLongitude; // place

    String type;
    double angleX;
    double angleY;
    double measurement;

    Sensor sensor;

    public Reading() {
    }

    public Reading(long time, double localizationLatitude, double localizationLongitude,
                   String type, double angleX, double angleY, double measurement) {
        this.time = time;
        this.localizationLatitude = localizationLatitude;
        this.localizationLongitude = localizationLongitude;
        this.type = type;
        this.angleX = angleX;
        this.angleY = angleY;
        this.measurement = measurement;
    }
}
