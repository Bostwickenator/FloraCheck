package org.bostwickenator.floracheck;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

public class CheckPlantService extends JobService implements BluetoothDataListener {
    private static final String TAG = "CheckPlantService";

    // Notification channel ID.
    private static final String PRIMARY_CHANNEL_ID =
            "primary_notification_channel";
    private NotificationManager mNotifyManager;
    private BluetoothLeComms bluetoothLeComms;
    private boolean evented = false;

    private JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        evented = false;

        Log.i(TAG, "on start job: " + params.getJobId());

        bluetoothLeComms = new BluetoothLeComms(this, false);
        bluetoothLeComms.addSwanDataListener(this);
        bluetoothLeComms.connect();
        createNotificationChannel();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "on stop job: " + params.getJobId());
        bluetoothLeComms.disconnect();
        return true;
    }

    private void createNotificationChannel() {

        // Define notification manager object.
        mNotifyManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Notification channels are only available in OREO and higher.
        // So, add a check on SDK version.
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O) {

            // Create the NotificationChannel with all the parameters.
            NotificationChannel notificationChannel = new NotificationChannel
                    (PRIMARY_CHANNEL_ID,
                            "Job Service notification",
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription
                    ("Notifications from Job Service");

            mNotifyManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onData(byte humidity) {
        if (!evented) {
            evented = true;
            bluetoothLeComms.disconnect();
            if (humidity < SettingsManager.getHumidity(this)) {
                //Set up the notification content intent to launch the app when clicked
                PendingIntent contentPendingIntent = PendingIntent.getActivity
                        (this, 0, new Intent(this, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT);

                Notification.Builder builder = new Notification.Builder(this, PRIMARY_CHANNEL_ID)
                        .setContentTitle("Plant needs water")
                        .setContentText("Soil moisture is only " + humidity + "%")
                        .setContentIntent(contentPendingIntent)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true);

                mNotifyManager.notify(0, builder.build());
            }
        }
        jobFinished(params, false);
    }

    @Override
    public void onConnectionStateUpdate(BluetoothConnectionState newState) {

    }
}
