package org.bostwickenator.floracheck;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements BluetoothDataListener {
    private BluetoothLeComms bluetoothLeComms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bluetoothLeComms = new BluetoothLeComms(this, true);
        bluetoothLeComms.addSwanDataListener(this);
        BootReceiver.schedule(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (checkPermissions()) {
            bluetoothLeComms.connect();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        bluetoothLeComms.disconnect();
    }

    private boolean checkPermissions() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                7);

    }

    @Override
    public void onData(final byte humidity) {
        final String toShow = "" + humidity + "%";
        final Context context = this;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (humidity < SettingsManager.getHumidity(context)) {
                    ((TextView) findViewById(R.id.textViewWeight)).setTextColor(getResources().getColor(R.color.warning));
                } else {
                    ((TextView) findViewById(R.id.textViewWeight)).setTextColor(getResources().getColor(R.color.colorPrimary));
                }
                ((TextView) findViewById(R.id.textViewWeight)).setText(toShow);
            }
        });
    }

    @Override
    public void onConnectionStateUpdate(final BluetoothConnectionState newState) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.textViewConnectionState)).setText(newState.name());
            }
        });
    }
}
