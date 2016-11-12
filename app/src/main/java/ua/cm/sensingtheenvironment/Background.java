package ua.cm.sensingtheenvironment;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.BoringLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.aflak.bluetooth.Bluetooth;
import ua.cm.sensingtheenvironment.database.Reading;
import ua.cm.sensingtheenvironment.database.Sensor;

// CREDITS http://stackoverflow.com/a/6592308
public class Background extends Service {
        private static Logger log = Logger.getLogger("SenseTheEnv");

    Handler scanTimer;
    Runnable scanAction;
    public static final int MSG_REGISTER_CLIENT = 0x00;
    public static final int NEXT_SCAN = 0x01;
    public static final int BEGIN_SCAN = 0x02;
    public static final int FINISH_SCAN = 0x03;
    public static final int ATT_CONNECTION = 0x04;
    public static final int PAIRING = 0x05;
    public static final int CONNECTED = 0x05;
    public static final int REQ_SCAN = 0x06;
    public static final int NEW_SENSOR = 0x10;
    public static final int NEAR_SENSOR = 0x11;
    public static final int REQ_SENSOR_INFO = 0x12;
    public static final int SENSOR_INFO = 0x13;
    public static final int REQ_FAILED = 0x20;
    public static final int SCAN_FAILED = 0x21;
    public static final int DISCONNECT = 0x22;
    public static final int CONNECT_FAILED = 0x23;
    public static final int INVALID_DATA = 0x24;


    // NOTE Might be overkill
    ArrayList<Messenger> clients = new ArrayList<Messenger>();
    // Target we publish for clients to send messages to
    final Messenger messenger = new Messenger(new IncomingHandler());

    private Bluetooth bluetooth;
    private String pendingReq = "";
    private int pendingReqAtt = 1;
    private Boolean blockScan = false;

    public Background() {
    }

    @Override
    public void onCreate()
    {
        log.log(Level.INFO, "Setup: ");
        bluetooth = new Bluetooth(this);
        bluetooth.enableBluetooth();
        scanTimer = new Handler();
        scanAction = new Runnable() {
            @Override
            public void run() {
                if (pendingReq.isEmpty()) {
                    sendMessage(BEGIN_SCAN, "", null);
                    log.log(Level.WARNING, "WE GOTTA SCAN: ");
                    bluetooth.scanDevices();
                }
            }
        };
        scanTimer.postDelayed(scanAction, 5000);

        bluetooth.setDiscoveryCallback(new Bluetooth.DiscoveryCallback() {

            @Override
            public void onFinish() {
                log.log(Level.WARNING, "Scan finished");
                if(!pendingReq.isEmpty() && pendingReqAtt < 3 && !blockScan)
                {
                    pendingReqAtt++;
                    sendMessage(ATT_CONNECTION, String.format(Locale.getDefault(), "(%s) Searching: %s", pendingReqAtt, pendingReq), null);
                    bluetooth.scanDevices();
                    return;
                }
                else if(pendingReqAtt >= 3 && !blockScan)
                {
                    sendMessage(REQ_FAILED, "Can't find device.", null);
                    restorePeriodicScan();
                }else if(blockScan)
                    return;
                scheduleNewScan();
            }
            public void onDevice(BluetoothDevice device) {
                log.log(Level.WARNING, "Found device: " + device.getName());
                if(!pendingReq.isEmpty())
                {
                    if(pendingReq.equals(device.getAddress()))
                    {
                        blockScan = true;
                        if(device.getBondState() == BluetoothDevice.BOND_NONE || device.getBondState() == BluetoothDevice.BOND_BONDING)
                        { //TODO Verify assumption we can request another pair while state=BONDING
                            log.log(Level.WARNING, "WE GOTTA PAIR: "+ device.getAddress());
                            bluetooth.pair(device); //Assume that we want the data on 1st pair
                            return;
                        }
                        log.log(Level.WARNING, "REQUEST CONNECTION: "+ device.getAddress());
                        bluetooth.connectToDevice(device);
                        return;
                    }
                }
                List<Sensor> sensors = Sensor.find(Sensor.class, " mac = ?", device.getAddress());
                if(sensors.size() <= 0) {
                    sendMessage(NEW_SENSOR, "", new Sensor(device.getAddress(), 0, 0, device.getName(), ""));
                }else{
                    sendMessage(NEAR_SENSOR, "", sensors.get(0));
                }
            }
            @Override
            public void onPair(BluetoothDevice device) {
                log.log(Level.WARNING, "PAIRED: "+ device.getName());
                sendMessage(PAIRING, device.getAddress(), null);
                bluetooth.connectToDevice(device);
            }
            @Override
            public void onUnpair(BluetoothDevice device) {
                log.log(Level.WARNING, "UNPAIRED: "+ device.getName());
            }
            @Override
            public void onError(String message)
            {
                sendMessage(SCAN_FAILED, message, null);
                restorePeriodicScan();
            }
        });
        bluetooth.setCommunicationCallback(new Bluetooth.CommunicationCallback() {
            @Override
            public void onConnect(BluetoothDevice device) {
                log.log(Level.WARNING, "WE GOTTA SEND: "+ device.getAddress());
                sendMessage(CONNECTED, String.format(Locale.getDefault(), "Connected: %s ", device.getAddress()), null);
                bluetooth.send("{\"GET\": [\"all\"]}\n");
            }

            @Override
            public void onDisconnect(BluetoothDevice device, String message) {
                sendMessage(DISCONNECT, String.format(Locale.getDefault(), "%s %s", device.getAddress(), message), null);
                restorePeriodicScan();
            }

            @Override
            public void onMessage(String message) {
                try {
                    Reading.fromJson(message, bluetooth.getDevice().getAddress());
                    sendMessage(SENSOR_INFO, bluetooth.getDevice().getAddress(), null );
                }catch (Exception e)
                {
                    sendMessage(INVALID_DATA, e.getMessage(), null);
                    return;
                }finally {
                    restorePeriodicScan();
                }
            }

            @Override
            public void onError(String message) {
                sendMessage(CONNECT_FAILED, message, null);
                restorePeriodicScan();
            }

            @Override
            public void onConnectError(BluetoothDevice device, String message) {
                sendMessage(CONNECT_FAILED, String.format(Locale.getDefault(), "%s %s", device.getAddress(), message), null);
                restorePeriodicScan();
            }
        });
    }

    private void restorePeriodicScan()
    {
        pendingReq = "";
        pendingReqAtt = 1;
        blockScan = false;
        scheduleNewScan();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
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
                case REQ_SENSOR_INFO:
                    log.log(Level.WARNING, "REQ: "+ (String)msg.obj);
                    scanTimer.removeCallbacks(scanAction);
                    Background.this.handleRequestSensor((String)msg.obj);
                    break;
                case REQ_SCAN:
                    scheduleNewScan(0);
                break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private void sendMessage(int msg_type, String content, Sensor s)
    { //TODO Overloading may be a better path, this one if string is present will always send the string
        for(Messenger client : clients) {
            try {
                Message msg = Message.obtain(null, msg_type);
                if(!content.isEmpty())
                    msg.obj = content;
                else if(s != null)
                    msg.obj = s;
                client.send(msg);
            } catch (RemoteException e) {
                // If we get here, the client is dead, and we should remove it from the list
                log.log(Level.INFO, "Removing client: " + client);
                clients.remove(client);
            }
        }
    }
    private void handleRequestSensor(String address)
    {
        Background.this.pendingReq = address;
        sendMessage(ATT_CONNECTION, String.format(Locale.getDefault(), "(%s) Searching: %s", pendingReqAtt, pendingReq), null);
        bluetooth.scanDevices();
    }
    private void scheduleNewScan()
    {
        scheduleNewScan(30);
    }
    private  void scheduleNewScan(int s)
    {
        if(blockScan || !pendingReq.isEmpty())
            return;
        scanTimer.removeCallbacks(scanAction);
        scanTimer.postDelayed(scanAction, s*1000L);
        for(Messenger client : clients) {
            try {
                Message msg = Message.obtain(null, NEXT_SCAN);
                msg.obj = s;
                client.send(msg);
            } catch (RemoteException e) {
                // If we get here, the client is dead, and we should remove it from the list
                log.log(Level.INFO, "Removing client: " + client);
                clients.remove(client);
            }
        }
    }
//    private class SearcherTask extends TimerTask{
//        @Override
//        public void run()
//        {
//            if(!(!pendingReq.isEmpty() || blockScan)) {
//                for(Messenger client : clients) {
//                    try {
//                        Message msg = Message.obtain(null, BEGIN_SCAN);
//                        client.send(msg);
//                    } catch (RemoteException e) {
//                        // If we get here, the client is dead, and we should remove it from the list
//                        log.log(Level.INFO, "Removing client: " + client);
//                        clients.remove(client);
//                    }
//                }
//                log.log(Level.WARNING, "WE GOTTA SCAN: ");
//                bluetooth.scanDevices();
//
//            }
////            List<BluetoothDevice> devices = bluetooth.getPairedDevices();
////            for(BluetoothDevice device : devices)
////            {
////                List<Sensor> s = Sensor.find(Sensor.class, "mac = ?", device.getAddress());
////                if(s.size() > 0)
////                {
////                    bluetooth.connectToDevice(device);
////                    yield halt/resume
////                }
////            }
//        }
//    }
}
