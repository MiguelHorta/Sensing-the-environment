package ua.cm.sensingtheenvironment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;

import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ua.cm.sensingtheenvironment.database.Sensor;

public class Feed extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String TAG = "SenseTheEnv";
    private static final String EVENT_LIST = "event_list";
    //private static final String BNDL_SERVICE = "service";
    Messenger networkService = null;
    final Messenger messenger = new Messenger(new IncomingHandler(new WeakReference<>(this)));
    final int REQUEST_ACCESS_COARSE_LOCATION = 0x00;

    public ArrayList<Event> getEventList() {
        return mEventList;
    }

    private ArrayList<Event> mEventList ;

    public AdapterEvent getAdapter() {
        return adapter;
    }

    AdapterEvent adapter;

    private double lastLongitude = 0;
    private double lastLatitude = 0;
    public View getMainView() {
        return coordinatorLayout;
    }

    private View coordinatorLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        coordinatorLayout = findViewById(R.id.content_feed_coordinator);
        Log.d(TAG, "Feed Setup^ ");
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
        ListView mEventView = (ListView) findViewById(R.id.event_list);
        if(savedInstanceState != null)
        {
            mEventList = savedInstanceState.getParcelableArrayList(EVENT_LIST);
        }else
        {
            mEventList = new ArrayList<>();
        }
        adapter = new AdapterEvent(this, R.layout.content_feed_item, mEventList);
        mEventView.setAdapter(adapter);
        Intent i = new Intent(this, Background.class);
        startService(i);
        bindService(i, networkServiceConnection, Context.BIND_AUTO_CREATE);

        i = new Intent(this, GPSService.class);
        startService(i);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(GPSService.GPS_LAST_LOCATION));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(EVENT_LIST, mEventList);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Feed Resume^ ");
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
                intent = new Intent(this, EditSensor.class);
                intent.putExtra(EditSensor.ARG_LATITUDE, lastLatitude);
                intent.putExtra(EditSensor.ARG_LONGITUDE, lastLongitude);
                startActivity(intent);
                break;
            case R.id.force_scan:
                try {
                    Message msg = Message.obtain(null, Background.REQ_SCAN);
                    networkService.send(msg);
                } catch (RemoteException e) {
                    // Here, the service has crashed even before we were able to connect
                }
                Snackbar.make(coordinatorLayout, "Forcing scan", Snackbar.LENGTH_LONG).show();
            break;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        Intent intent;
        switch(item.getItemId())
        {
            case R.id.nav_map:
                intent = new Intent(Feed.this, MapsActivity.class);
                intent.putExtra(GPSService.GPS_LATITUDE, lastLatitude);
                intent.putExtra(GPSService.GPS_LONGITUDE, lastLongitude);
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
            case R.id.nav_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            break;
            case R.id.nav_quit:
                intent = new Intent(this, Background.class);
                stopService(intent);
                intent = new Intent(this, Background.class);
                stopService(intent);
                this.finish();
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
                Log.d(TAG, "Connected to service");
            } catch (RemoteException e) {
                // Here, the service has crashed even before we were able to connect
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            networkService = null;
            Log.d(TAG, "Disconnected from service");
        }
    };
    static class IncomingHandler extends Handler {
        private final WeakReference<Feed> ref;

        IncomingHandler(WeakReference<Feed> ref) {
            this.ref = ref;
        }

        private CountDownTimer countdown = null;
        @Override
        public void handleMessage(Message msg) {
            if(ref.get() == null)
            {
                return;
            }
            if(countdown != null)
                countdown.cancel(); //TODO Verify the assumption: Every time this function is called countdown needs to reset
            switch (msg.what) {
                case Background.SENSOR_INFO:
                    ref.get().getEventList().add(0, new Event(Background.SENSOR_INFO, msg.obj, ref.get()));
                    ref.get().getAdapter().notifyDataSetChanged();
                    ((TextView)ref.get().findViewById(R.id.content_status_bar)).setText(String.format(Locale.getDefault(), "Received from: %s", (String)msg.obj));
                    break;
                case Background.NEW_SENSOR:
                    ref.get().getEventList().add(0, new Event(Background.NEW_SENSOR, msg.obj, ref.get()));
                    ref.get().getAdapter().notifyDataSetChanged();
                    break;
                case Background.NEAR_SENSOR:
                    ref.get().getEventList().add(0, new Event(Background.NEAR_SENSOR, msg.obj, ref.get()));
                    ref.get().getAdapter().notifyDataSetChanged();
                    break;
                case Background.NEXT_SCAN:
                    Log.d(TAG, "SCAN in: "+(String)msg.obj);
                    countdown = new CountDownTimer((Integer.parseInt((String)msg.obj))*1000 , 1000) {
                        public void onTick(long millisUntilFinished) {
                            if(ref.get() != null)
                                ((TextView) ref.get().findViewById(R.id.content_status_bar)).setText(String.format(Locale.getDefault(), "Next Scan in: %d", millisUntilFinished / 1000));
                        }

                        public void onFinish() {
                            if(ref.get() != null)
                                ((TextView) ref.get().findViewById(R.id.content_status_bar)).setText( ref.get().getString(R.string.scanning));
                        }
                    }.start();
                    break;
                case Background.BEGIN_SCAN:
                    ((TextView) ref.get().findViewById(R.id.content_status_bar)).setText( ref.get().getString(R.string.scanning));
                    break;
                case Background.FINISH_SCAN:
                    ((TextView) ref.get().findViewById(R.id.content_status_bar)).setText(ref.get().getString(R.string.finish_scanning));
                    break;
                case Background.PAIRING:
                    ((TextView)ref.get().findViewById(R.id.content_status_bar)).setText(String.format(Locale.getDefault(), "Pairing with: %s", (String)msg.obj));
                    break;
                case Background.CONNECTED:
                    ((TextView)ref.get().findViewById(R.id.content_status_bar)).setText((String)msg.obj);
                    break;
                case Background.ATT_CONNECTION:
                    ((TextView)ref.get().findViewById(R.id.content_status_bar)).setText((String)msg.obj);
                    break;
                case Background.INITIALIZING:
                    ((TextView)ref.get().findViewById(R.id.content_status_bar)).setText(ref.get().getString(R.string.initializing));
                    break;
                case Background.CONNECT_FAILED:
                case Background.DISCONNECT:
                case Background.INVALID_DATA:
                case Background.REQ_FAILED:
                case Background.SCAN_FAILED:
                    Snackbar.make(ref.get().getMainView(), (String)msg.obj, Snackbar.LENGTH_LONG).show();
                break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class AdapterEvent extends ArrayAdapter<Event> {
        AdapterEvent(Context context, int resource, List<Event> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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
                        switch (item.getEventType()) {
                            case Background.NEW_SENSOR:
                                Intent i = new Intent(Feed.this, EditSensor.class);
                                i.putExtra(EditSensor.ARG_LATITUDE, lastLatitude);
                                i.putExtra(EditSensor.ARG_LONGITUDE, lastLongitude);
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
                                    ((TextView)findViewById(R.id.content_status_bar)).setText(String.format(Locale.getDefault(), "Asking: %s", (String)msg.obj));
                                } catch (RemoteException e) {
                                   // Here, the service has crashed even before we were able to connect
                                }
                            break;
                        }
                    }
                });
                return v;
            } catch (Exception ex) {
                Log.d(TAG, "Error populating feed list");
                return convertView;
            }
        }

    }

    @Override
    protected void onDestroy() {
        try {
            Message msg = Message.obtain(null, Background.DELETE_REFERENCE);
            // TODO getDesc shouldn't be used like this
            msg.replyTo = messenger;
            networkService.send(msg);
        } catch (RemoteException e) {
            // Here, the service has crashed even before we were able to connect
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        unbindService(networkServiceConnection);
        super.onDestroy();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "GOT POS*************");
            Bundle b = intent.getBundleExtra(GPSService.GPS_LOCATION);
            lastLatitude = b.getDouble(GPSService.GPS_LATITUDE, 0);
            lastLongitude = b.getDouble(GPSService.GPS_LONGITUDE, 0);
        }
    };
}
class Event implements Parcelable {
    private int event_type;
    private String title;
    private String description;
    private String action;
    private String context_title;
    private Long time;

    Event(int event_type, Object obj, Context ctx) {
        this.event_type = event_type;
        this.time = System.currentTimeMillis()/1000;
        switch (event_type)
        {
            case Background.SENSOR_INFO:
                title =  ((String)obj);
                description = ((String)obj);
                action = ctx.getString(R.string.click_to_details);
                context_title = ctx.getString(R.string.new_info);
                break;
            case Background.NEAR_SENSOR:
                title =  ((Sensor)obj).getGivenName();
                description = ((Sensor)obj).getMAC();
                action =  ctx.getString(R.string.click_to_request);
                context_title = ctx.getString(R.string.near_sensor);
                break;
            case Background.NEW_SENSOR:
                title =  ((Sensor)obj).getGivenName();
                description = ((Sensor)obj).getMAC();
                action = ctx.getString(R.string.click_to_connect);
                context_title = ctx.getString(R.string.found_device);
                break;
            default:
                title = "Default";
                description = "Default";
                action = "Default";
                context_title= "Default";
        }
    }

    int getEventType() {
        return event_type;
    }

    public String getTitle()
    {
        return title;
    }
    String getDesc()
    {
        return description;
    }

    public String getAction()
    {
        return action;
    }
    String getEvent()
    {
        return context_title;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(event_type);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(action);
        dest.writeString(context_title);
        dest.writeLong(time);

    }

    public static final Parcelable.Creator<Event> CREATOR
            = new Parcelable.Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    private Event(Parcel in)
    {
        event_type = in.readInt();
        title = in.readString();
        description = in.readString();
        action = in.readString();
        context_title = in.readString();
        time = in.readLong();
    }

}