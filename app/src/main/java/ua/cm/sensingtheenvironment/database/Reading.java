package ua.cm.sensingtheenvironment.database;

import android.bluetooth.BluetoothDevice;

import com.orm.SugarRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by horta on 30-10-2016.
 */

/***
 * Implemented the readings as easily as possibly.
 * It was attempted to find a standard or recommendation but nothing come up.
 * We settled for the minimum functionality.
 */
public class Reading extends SugarRecord {
    public static final String MSR_TEMPERATURE = "TEMPERATURE";
    public static final String MSR_HUMIDITY = "HUMIDITY";

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

    public String getType()
    {
        return type;
    }

    public long getTimestamp()
    {
        return time;
    }

    public static void fromJson(String message, String addr) throws JSONException
    {
        try {
            JSONObject jObject = new JSONObject(message);
            JSONArray measures = jObject.getJSONArray("MEASURES");
            for(int i=0; i < measures.length(); i++)
            {
                JSONObject measure = measures.getJSONObject(i);
                Reading r;
                switch (measure.getString("TYPE"))
                {
                    case MSR_TEMPERATURE:
                        r = new Reading(measure.optLong("TIME", System.currentTimeMillis()/1000),
                                measure.optDouble("LATITUDE", 0),
                                measure.optDouble("LONGITUDE", 0),
                                measure.optString("TYPE", "INVALID"),
                                0,
                                0,
                                measure.optDouble("MEASUREMENT_X", 25),
                                0,
                                0,
                                Sensor.find(Sensor.class, "mac = ?", addr).get(0));
                        r.save();
                        break;
                    case MSR_HUMIDITY:
                        r = new Reading(measure.optLong("TIME", System.currentTimeMillis()/1000),
                                measure.optDouble("LATITUDE", 0),
                                measure.optDouble("LONGITUDE", 0),
                                measure.optString("TYPE", "INVALID"),
                                0,
                                0,
                                measure.optDouble("MEASUREMENT_X", 25),
                                0,
                                0,
                                Sensor.find(Sensor.class, "mac = ?", addr).get(0));
                        r.save();
                        break;
                }
            }
        }catch (JSONException e){
            throw e;
        }

    }

}
