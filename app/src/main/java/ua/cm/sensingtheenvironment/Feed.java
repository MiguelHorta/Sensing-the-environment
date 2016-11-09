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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Feed extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    Messenger networkService = null;
    final Messenger messenger = new Messenger(new IncomingHandler());
    private static Logger log = Logger.getLogger("SenseTheEnv");
    private final int REQUEST_ACCESS_COARSE_LOCATION = 0x00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeder);
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
        Intent i = new Intent(this, Background.class);
        startService(i);
        bindService(i, networkServiceConnection, Context.BIND_AUTO_CREATE);
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
        getMenuInflater().inflate(R.menu.feeder, menu);
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
                Toast.makeText(this, "¡¡NOT IMPLEMENTED!!", Toast.LENGTH_SHORT)
                        .show();
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
                        .setMessage(R.string.onfirmation_delete_sensors)
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
                    // do something here
                    break;
                case Background.NEW_SENSOR:
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
