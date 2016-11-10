package ua.cm.sensingtheenvironment;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ua.cm.sensingtheenvironment.database.Sensor;

public class EditSensor extends AppCompatActivity {
    private static Logger log = Logger.getLogger("SenseTheEnv");
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lng";
//    public static final String ARG_MAC = "mac";
//    public static final String ARG_NAME = "name";
//    public static final String ARG_DESCRIPTION = "desc";
    public static final String ARG_SENSOR_ID = "id";

    private Sensor s = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_sensor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.edit_sensor_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        Bundle extras = getIntent().getExtras();
        if(extras != null ) {
            ((EditText)findViewById(R.id.latitude_edit)).setText(extras.getString(ARG_LATITUDE, "0"));
            ((EditText)findViewById(R.id.longitude_edit)).setText(extras.getString(ARG_LONGITUDE, "0"));
            if (extras.containsKey(ARG_SENSOR_ID)) {
                s = Sensor.findById(Sensor.class, extras.getLong(ARG_SENSOR_ID));
                ((EditText)findViewById(R.id.sensor_name_edit)).setText(s.getGivenName());
                ((EditText)findViewById(R.id.mac_address_edit)).setText(s.getMAC());
                ((EditText)findViewById(R.id.description_edit)).setText(s.getDesc());
                ((EditText)findViewById(R.id.latitude_edit)).setText(String.valueOf(s.getLatitude()));
                ((EditText)findViewById(R.id.longitude_edit)).setText(String.valueOf(s.getLongitude()));
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sensor_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.sensor_edit_save:
                log.log(Level.INFO, "SAVE");
                String name = ((TextView)findViewById(R.id.sensor_name_edit)).getText().toString();
                name = name.isEmpty() ? "UnNamed": name;
                String mac = ((TextView) findViewById(R.id.mac_address_edit)).getText().toString();
                if (mac.length() != 2 * 6 + 5) {
                    Toast.makeText(this, "Bad mac, failed.", Toast.LENGTH_SHORT)
                            .show();
                    break;
                }
                try {
                    if(s == null)
                        s = new Sensor();
                    s.rePopulate(mac, Double.parseDouble(((TextView)findViewById(R.id.latitude_edit)).getText().toString()),
                            Double.parseDouble(((TextView)findViewById(R.id.longitude_edit)).getText().toString()),
                            name,
                            ((TextView)findViewById(R.id.description_edit)).getText().toString());
                    s.save();
                    this.finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
