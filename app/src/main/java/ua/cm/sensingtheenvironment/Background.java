package ua.cm.sensingtheenvironment;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

public class Background extends Service {
    public Background() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                //performe the deskred task
            }
        }, 60*5*1000);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
