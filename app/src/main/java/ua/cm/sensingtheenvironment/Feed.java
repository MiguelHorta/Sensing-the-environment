package ua.cm.sensingtheenvironment;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ua.cm.sensingtheenvironment.database.Sensor;

public class Feed extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    Messenger networkService = null;
    final Messenger messenger = new Messenger(new IncomingHandler());
    private final int REQUEST_ACCESS_COARSE_LOCATION = 0x00;

    private ArrayList<Event> mEventList= new ArrayList<>();
    AdapterEvent adapter;

    private static Logger log = Logger.getLogger("SenseTheEnv");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // CREDITS http://stackoverflow.com/a/36177638
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            REQUEST_ACCESS_COARSE_LOCATION);
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
        }

        ListView mEventView = (ListView)findViewById(R.id.event_list);
        adapter = new AdapterEvent(this, R.layout.content_feed_item , mEventList);
        mEventView.setAdapter(adapter);


        Intent i = new Intent(this, Background.class);
        startService(i);
        bindService(i, networkServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        adapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feed, menu);
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
            case R.id.add_sensor:
                Intent i = new Intent(this, EditSensor.class);
                i.putExtra(EditSensor.ARG_LATITUDE, 0);
                i.putExtra(EditSensor.ARG_LONGITUDE, 0);
                startActivity(i);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Intent intent;
        switch(item.getItemId())
        {
            case R.id.nav_map:
                intent = new Intent(Feed.this, MapsActivity.class);
                startActivity(intent);
                break;
            // action with ID action_settings was selected
            case R.id.nav_list_sensors:
                intent = new Intent(this, SensorListActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_list_readings:
                intent = new Intent(this, ReadingListActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_delete:
                // TODO MOVE TO SETTINGS ACTIVITY
                // CREDITS http://stackoverflow.com/a/5127506
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.confirmation_delete_sensors)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                Toast.makeText(Feed.this, R.string.success_delete_sensors, Toast.LENGTH_SHORT).show();
                                //TODO DELETE QUERY
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private ServiceConnection networkServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            networkService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, Background.MSG_REGISTER_CLIENT);
                msg.replyTo = messenger;
                networkService.send(msg);
                log.log(Level.INFO, "Connected to service");
            } catch (RemoteException e) {
                // Here, the service has crashed even before we were able to connect
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            networkService = null;
            log.log(Level.INFO, "Disconnected from service");
        }
    };
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Background.SENSOR_INFO:
                    mEventList.add(0, new Event(Background.SENSOR_INFO, msg.obj));
                    adapter.notifyDataSetChanged();
                    break;
                case Background.NEW_SENSOR:
                    mEventList.add(0, new Event(Background.NEW_SENSOR, msg.obj));
                    adapter.notifyDataSetChanged();
                case Background.NEAR_SENSOR:
                    mEventList.add(0, new Event(Background.NEAR_SENSOR, msg.obj));
                    adapter.notifyDataSetChanged();
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class AdapterEvent extends ArrayAdapter<Event> {
        public AdapterEvent(Context context, int resource, List<Event> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                final Event item = getItem(position);
                View v = null;
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    v = inflater.inflate(R.layout.content_feed_item, null);

                } else {
                    v = convertView;
                }

                TextView mEventTitle = (TextView) v.findViewById(R.id.event_title);
                TextView mEventDesc = (TextView) v.findViewById(R.id.event_desc);
                TextView mEventAction = (TextView) v.findViewById(R.id.event_action);

                mEventTitle.setText(item.getEvent() + " " + item.getTitle());
                mEventDesc.setText(item.getDesc());
                mEventAction.setText(item.getAction());
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (item.event_type) {
                            case Background.NEW_SENSOR:
                                Intent i = new Intent(Feed.this, EditSensor.class);
                                i.putExtra(EditSensor.ARG_LATITUDE, 0);
                                i.putExtra(EditSensor.ARG_LONGITUDE, 0);
                                i.putExtra(EditSensor.ARG_NAME, item.getTitle());
                                i.putExtra(EditSensor.ARG_MAC, item.getDesc());
                                startActivity(i);
                                break;
                            case Background.NEAR_SENSOR:
                                try {
                                    Message msg = Message.obtain(null, Background.REQ_SENSOR_INFO);
                                    // TODO getDesc shouldn't be used like this
                                    msg.obj = item.getDesc();
                                    networkService.send(msg);
                                } catch (RemoteException e) {
                                   // Here, the service has crashed even before we were able to connect
                                }
                            break;
                        }
                    }
                });
                return v;
            } catch (Exception ex) {
                log.log(Level.INFO, "Error populating feed list");
                return convertView;
            }
        }

    }

    private class Event {
        public Event(int event_type, Object ob) {
            this.event_type = event_type;
            this.obj = ob;
        }

        public int getEvent_type() {
            return event_type;
        }

        public Object getObj() {
            return obj;
        }

        public String getTitle()
        {
            switch (event_type)
            {
                case Background.SENSOR_INFO:
                case Background.NEAR_SENSOR:
                case Background.NEW_SENSOR:
                    return ((Sensor)obj).getGivenName();
                default:
                    return "Default";
            }
        }
        public String getDesc()
        {
            switch (event_type)
            {
                case Background.SENSOR_INFO:
                case Background.NEAR_SENSOR:
                case Background.NEW_SENSOR:
                    Sensor s = ((Sensor)obj);
                    return s.getMAC();
                default:
                    return "Default";
            }
        }

        public String getAction()
        {
            switch (event_type)
            {
                case Background.SENSOR_INFO:
                    return getString(R.string.click_to_details);
                case Background.NEAR_SENSOR:
                    return getString(R.string.click_to_request);
                case Background.NEW_SENSOR:
                    return getString(R.string.click_to_connect);
                default:
                    return "Default";
            }
        }
        public String getEvent()
        {
            switch (event_type)
            {
                case Background.SENSOR_INFO:
                    return getString(R.string.new_info);
                case Background.NEAR_SENSOR:
                    return getString(R.string.near_sensor);
                case Background.NEW_SENSOR:
                    return getString(R.string.found_device);
                default:
                    return "Default";
            }
        }

        private int event_type;
        private Object obj;
    }

    @Override
    protected void onDestroy() {
        unbindService(networkServiceConnection);
        super.onDestroy();
    }
}
