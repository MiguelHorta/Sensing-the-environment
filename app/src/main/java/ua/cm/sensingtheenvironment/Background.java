package ua.cm.sensingtheenvironment;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.content.Context;
import android.os.Message;
import android.os.Messenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.aflak.bluetooth.Bluetooth;

// CREDITS http://stackoverflow.com/a/6592308
public class Background extends Service {
    private static Logger log = Logger.getLogger("SenseTheEnv");

    Timer timer;
    SearcherTask searchTask;

    public static final int MSG_REGISTER_CLIENT = 0x00;
    public static final int NEW_SENSOR = 0x10;
    public static final int SENSOR_INFO = 0x11;
    // NOTE Might be overkill
    ArrayList<Messenger> clients = new ArrayList<Messenger>();
    // Target we publish for clients to send messages to
    final Messenger messenger = new Messenger(new IncomingHandler());

    private Bluetooth bluetooth;
    public Background() {
    }

    @Override
    public void onCreate()
    {
        bluetooth = new Bluetooth(this);
        bluetooth.enableBluetooth();
        timer = new Timer();
        searchTask= new SearcherTask();

        //delay 1s, repeat in 30s
        timer.schedule(searchTask, 1000, 30*1000);

        bluetooth.setDiscoveryCallback(new Bluetooth.DiscoveryCallback() {

            @Override
            public void onFinish() {
                log.log(Level.WARNING, "Scan finished");
            }
            public void onDevice(BluetoothDevice device) {
                log.log(Level.WARNING, "Found device: " + device.getName());
            }
            @Override
            public void onPair(BluetoothDevice device) {
                log.log(Level.WARNING, "PAIRED: "+ device.getName());
            }
            @Override
            public void onUnpair(BluetoothDevice device) {
                log.log(Level.WARNING, "UNPAIRED: "+ device.getName());
            }
            @Override
            public void onError(String message) {
                log.log(Level.WARNING, "ERROR: "+ message);
            }
        });
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    log.log(Level.INFO, "Adding client: " + msg.replyTo);
                    clients.add(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private class SearcherTask extends TimerTask{
        @Override
        public void run() {
            bluetooth.scanDevices();
        }
    }
}
