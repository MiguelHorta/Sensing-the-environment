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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.aflak.bluetooth.Bluetooth;
import ua.cm.sensingtheenvironment.database.Reading;
import ua.cm.sensingtheenvironment.database.Sensor;

// CREDITS http://stackoverflow.com/a/6592308
public class Background extends Service {
    private static Logger log = Logger.getLogger("SenseTheEnv");

    Timer timer;
    SearcherTask searchTask;

    public static final int MSG_REGISTER_CLIENT = 0x00;
    public static final int NEW_SENSOR = 0x10;
    public static final int NEAR_SENSOR = 0x11;
    public static final int REQ_SENSOR_INFO = 0x12;
    public static final int SENSOR_INFO = 0x13;
    // NOTE Might be overkill
    ArrayList<Messenger> clients = new ArrayList<Messenger>();
    // Target we publish for clients to send messages to
    final Messenger messenger = new Messenger(new IncomingHandler());

    private Bluetooth bluetooth;
    private String pendingReq = "";
    private int pendingReqAtt = 0;
    private Boolean blockScan = false;

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
                if(!pendingReq.isEmpty())
                {
                    pendingReqAtt++;
                    bluetooth.scanDevices();
                }
                if(pendingReqAtt > 3) {
                    //TODO INFORM THE USER IT FAILED
                    pendingReq = "";
                    pendingReqAtt = 0;
                }
            }
            public void onDevice(BluetoothDevice device) {
                log.log(Level.WARNING, "Found device: " + device.getName());
                if(!pendingReq.isEmpty())
                {
                    if(pendingReqAtt < 3 && pendingReq.equals(device.getAddress()))
                    {
                        blockScan = true;
                        if(device.getBondState() == BluetoothDevice.BOND_NONE)
                        {
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
                    for(Messenger client : clients) {
                        try {
                            Message msg = Message.obtain(null, NEW_SENSOR);
                            msg.obj = new Sensor(device.getAddress(), 0, 0, device.getName(), "");
                            client.send(msg);
                        } catch (RemoteException e) {
                            // If we get here, the client is dead, and we should remove it from the list
                            log.log(Level.INFO, "Removing client: " + client);
                            clients.remove(client);
                        }
                    }
                }else{
                    for(Messenger client : clients) {
                        try {
                            Message msg = Message.obtain(null, NEAR_SENSOR);
                            msg.obj = sensors.get(0);
                            client.send(msg);
                        } catch (RemoteException e) {
                            // If we get here, the client is dead, and we should remove it from the list
                            log.log(Level.INFO, "Removing client: " + client);
                            clients.remove(client);
                        }
                    }
                }
            }
            @Override
            public void onPair(BluetoothDevice device) {
                log.log(Level.WARNING, "PAIRED: "+ device.getName());
                bluetooth.connectToDevice(device);
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
        bluetooth.setCommunicationCallback(new Bluetooth.CommunicationCallback() {
            @Override
            public void onConnect(BluetoothDevice device) {
                log.log(Level.WARNING, "WE GOTTA SEND: "+ device.getAddress());
                bluetooth.send("{\"GET\": [\"all\"]}\n");
            }

            @Override
            public void onDisconnect(BluetoothDevice device, String message) {
                log.log(Level.WARNING, "WE GOTTA KICK: "+ device.getAddress()+message);
                pendingReq = "";
                pendingReqAtt = 0;
                blockScan = false;
            }

            @Override
            public void onMessage(String message) {
                try {
                    Reading.fromJson(message, bluetooth.getDevice().getAddress());
                }catch (Exception e)
                {
                    //TODO inform the user
                    return;
                }
                for(Messenger client : clients) {
                    try {
                        Message msg = Message.obtain(null, SENSOR_INFO);
                        msg.obj = bluetooth.getDevice().getAddress();
                        client.send(msg);
                    } catch (RemoteException e) {
                        // If we get here, the client is dead, and we should remove it from the list
                        log.log(Level.INFO, "Removing client: " + client);
                        clients.remove(client);
                    }
                }
                pendingReq = "";
                pendingReqAtt = 0;
                blockScan = false;
            }

            @Override
            public void onError(String message) {
                log.log(Level.WARNING, "ERROR: "+ message);
            }

            @Override
            public void onConnectError(BluetoothDevice device, String message) {
                log.log(Level.WARNING, "WE GOTTA KICKROUND: "+ device.getAddress()+message);
                pendingReq = "";
                pendingReqAtt = 0;
                blockScan = false;
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
                case REQ_SENSOR_INFO:
                    log.log(Level.WARNING, "REQ: "+ (String)msg.obj);
                    Background.this.handleRequestSensor((String)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private void handleRequestSensor(String address)
    {
        Background.this.pendingReq = address;
        bluetooth.scanDevices();
    }

    private class SearcherTask extends TimerTask{
        @Override
        public void run()
        {
            if(!(!pendingReq.isEmpty() || blockScan)) {
                log.log(Level.WARNING, "WE GOTTA SCAN: ");
                bluetooth.scanDevices();

            }
//            List<BluetoothDevice> devices = bluetooth.getPairedDevices();
//            for(BluetoothDevice device : devices)
//            {
//                List<Sensor> s = Sensor.find(Sensor.class, "mac = ?", device.getAddress());
//                if(s.size() > 0)
//                {
//                    bluetooth.connectToDevice(device);
//                    yield halt/resume
//                }
//            }
        }
    }
}
