package org.bostwickenator.floracheck;

interface BluetoothDataListener {

    void onData(byte humidity);

    void onConnectionStateUpdate(BluetoothConnectionState newState);
}
