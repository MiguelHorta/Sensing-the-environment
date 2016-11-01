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
    double angleXoY;
    double angleXoZ;
    double measurementX;
    double measurementY;
    double measurementZ;

    Sensor sensor;

    public Reading() {
    }

    public Reading(long time, double localizationLatitude, double localizationLongitude, String type, double angleXoY, double angleXoZ, double measurementX, double measurementY, double measurementZ, Sensor sensor) {
        this.time = time;
        this.localizationLatitude = localizationLatitude;
        this.localizationLongitude = localizationLongitude;
        this.type = type;
        this.angleXoY = angleXoY;
        this.angleXoZ = angleXoZ;
        this.measurementX = measurementX;
        this.measurementY = measurementY;
        this.measurementZ = measurementZ;
        this.sensor = sensor;
    }
}
