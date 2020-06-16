package org.bostwickenator.floracheck;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    public static void schedule(Context context) {
        ComponentName serviceName = new ComponentName(context.getPackageName(),
                CheckPlantService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceName);
        builder.setPeriodic(1000 * 60);
        JobInfo myJobInfo = builder.build();
        JobScheduler mScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        mScheduler.schedule(myJobInfo);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            try {
                Log.i(TAG, "Schedule job");
                schedule(context);
                Log.i(TAG, "Schedule complete");

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}