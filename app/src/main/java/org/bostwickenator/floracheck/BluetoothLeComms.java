package org.bostwickenator.floracheck;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class BluetoothLeComms {

    private final static UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private UUID FLORA_SERVICE_UUID = UUID.fromString("00001204-0000-1000-8000-00805f9b34fb");
    private UUID FLORA_CHARACTERISTIC_UUID = UUID.fromString("00001a01-0000-1000-8000-00805f9b34fb");
    private UUID MODE_UUID = UUID.fromString("00001a00-0000-1000-8000-00805f9b34fb");
    private UUID OTHER_SERVICE_UUID = UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb");
    private UUID OTHER_CHAR1_UUID = UUID.fromString("00000001-0000-1000-8000-00805f9b34fb");
    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGattCallback mBluetoothGattCallback;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic floraCharacteristic;
    private List<BluetoothDataListener> mListeners;
    private BluetoothConnectionState mConnectionState;
    private boolean shouldConnect = false;
    boolean lowLatency = false;
    private Handler bleHandler;
    private BluetoothGattCharacteristic mode;

    public BluetoothLeComms(Context context, boolean lowLatency) {
        mContext = context;
        this.lowLatency = lowLatency;
        mListeners = new ArrayList<>();
        bleHandler = new Handler(Looper.getMainLooper());
    }

    private static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    public void connect() {
        shouldConnect = true;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();


        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            connectionStateUpdate(BluetoothConnectionState.BLUETOOTH_DISABLED);
            return;
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        createScanCallback();
        createBluetoothGattCallback();
        beginScan();
    }

    public void disconnect() {
        shouldConnect = false;
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    public void addSwanDataListener(BluetoothDataListener listener) {
        mListeners.add(listener);
    }

    private void beginScan() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        if(lowLatency){
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        }
        mBluetoothLeScanner.startScan(Collections.singletonList(getSwanScanFilter()),  builder.build(), mScanCallback);
        connectionStateUpdate(BluetoothConnectionState.SEARCHING);
        mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
    }

    private void createBluetoothGattCallback() {
        if (mBluetoothGattCallback != null)
            return;
        mBluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                bleHandler.post(new Runnable() {
                    public void run() {
                        setupToReceiveNotifications();
                    }
                });
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                bleHandler.post(new Runnable() {
                    public void run() {
                        processData();
                    }
                });
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                bleHandler.post(new Runnable() {
                    public void run() {
                        BluetoothGattService otherS1 = mBluetoothGatt.getService(OTHER_SERVICE_UUID);
                        enableNotification(otherS1.getCharacteristic(OTHER_CHAR1_UUID)); // 13
                    }
                });
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                if (descriptor.getCharacteristic().getUuid().equals(floraCharacteristic.getUuid())) {
                    bleHandler.post(new Runnable() {
                        public void run() {
                            mode.setValue(hexStringToByteArray("A01F"));
                            boolean success = mBluetoothGatt.writeCharacteristic(mode);
                            System.out.println("Wrote magic bytes: " + success);
                        }
                    });
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                switch (newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        connectionStateUpdate(BluetoothConnectionState.CONNECTING);
                        mBluetoothGatt = gatt;
                        gatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        connectionStateUpdate(BluetoothConnectionState.DISCONNECTED);
                        if (shouldConnect) {
                            beginScan();
                        }
                        break;
                }
            }
        };
    }

    private void processData() {
        byte[] value = floraCharacteristic.getValue();

        System.out.println(Utils.bytesToHex(value));
        System.out.println("moisture % " + (value[7]));

        for (BluetoothDataListener d : mListeners
        ) {
            d.onData(value[7]);
        }

        /*
        Little endian encoding
        bytes   0-1: temperature in 0.1 °C
        byte      2: unknown
        bytes   3-6: brightness in Lux (MiFlora only)
        byte      7: moisture in %
        byted   8-9: conductivity in µS/cm
        bytes 10-15: unknown
        */
    }

    private void connectionStateUpdate(BluetoothConnectionState newState) {
        mConnectionState = newState;
        for (BluetoothDataListener listener : mListeners) {
            listener.onConnectionStateUpdate(newState);
        }
    }

    private void setupToReceiveNotifications() {
        BluetoothGattService floraService = mBluetoothGatt.getService(FLORA_SERVICE_UUID);
        floraCharacteristic = floraService.getCharacteristic(FLORA_CHARACTERISTIC_UUID);
        mode = floraService.getCharacteristic(MODE_UUID);
        enableNotification(floraCharacteristic);
        connectionStateUpdate(BluetoothConnectionState.CONNECTED);
    }

    private void enableNotification(BluetoothGattCharacteristic cha) {
        mBluetoothGatt.setCharacteristicNotification(cha, true);

        BluetoothGattDescriptor descriptor = cha.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            System.out.println("write descriptor succcess: " + mBluetoothGatt.writeDescriptor(descriptor));

        }
    }

    private void createScanCallback() {
        if (mScanCallback != null)
            return;
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                mBluetoothLeScanner.stopScan(mScanCallback);
                mBluetoothDevice = result.getDevice();
                mBluetoothDevice.connectGatt(mContext, true, mBluetoothGattCallback);
            }
        };
    }

    private ScanFilter getSwanScanFilter() {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceName("Flower care");
        //builder.setDeviceAddress("C4:7C:8D:67:27:ED");
        return builder.build();
    }
}
