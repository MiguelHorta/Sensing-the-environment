package ua.cm.sensingtheenvironment.database;

import com.orm.SugarRecord;
import com.orm.dsl.NotNull;
import com.orm.dsl.Unique;

import java.util.List;

/**
 * Created by horta on 30-10-2016.
 */

public class Sensor extends SugarRecord {
    // id auto
    @Unique
    @NotNull
    String mac; // MAC address, unless some chinese chip is used must be unique
    double localizationLatitude; // Last place we had connectivity
    double localizationLongitude; // Last place we had connectivity

    @Unique
    String givenName;
    String description;

    public Sensor()
    {}

    public Sensor(String mac, double localizationLatitude, double localizationLongitude, String givenName, String description)
    {
        this.mac = mac;
        this.localizationLatitude = localizationLatitude;
        this.localizationLongitude = localizationLongitude;
        this.givenName = givenName;
        this.description = description;
    }

    List<Reading> getReadings()
    {
        return Reading.find(Reading.class, "reading = ?", new String[]{getId().toString()});
    }
}
