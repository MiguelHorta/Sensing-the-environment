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
import java.util.logging.Level;
import java.util.logging.Logger;

import me.aflak.bluetooth.Bluetooth;

// CREDITS http://stackoverflow.com/a/6592308
public class Background extends Service {
    private static Logger log = Logger.getLogger("SenseTheEnv");

    public static final int MSG_REGISTER_CLIENT = 0x00;
    public static final int SENSOR_INFO = 0x10;
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
        bluetooth.setDiscoveryCallback(new Bluetooth.DiscoveryCallback() {

            @Override
            public void onFinish() {
                // scan finished
            }
            @Override
            public void onDevice(BluetoothDevice device) {
                // device found
            }
            @Override
            public void onPair(BluetoothDevice device) {
                // device paired
            }
            @Override
            public void onUnpair(BluetoothDevice device) {
                // device unpaired
            }
            @Override
            public void onError(String message) {
                // error occurred
            }
        });
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // CREDITS http://stackoverflow.com/a/28062885
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                log.log(Level.INFO, "--> UPDATED\n");
                List<BluetoothDevice> devices = bluetooth.getPairedDevices();
                for (BluetoothDevice device: devices) {
                    bluetooth.send("UPDATE\n");
                }
            }
        }, 60*5*1000);

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
}
