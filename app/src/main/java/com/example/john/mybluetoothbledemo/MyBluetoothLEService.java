package com.example.john.mybluetoothbledemo;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.UUID;

/**
 * Providing the service that connects the UX device as a client to the peripheral Bluetooth LE Sensor
 *
 * Helpful Resources:
 * https://medium.com/@avigezerit/bluetooth-low-energy-on-android-22bc7310387a
 * https://developer.android.com/guide/topics/connectivity/bluetooth-le#notification
 * https://www.bluetooth.com/specifications/gatt
 */
public class MyBluetoothLEService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Connection Lifecycle
    public void connect(Context context, boolean autoConnect, BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(context, autoConnect, mGattCallback);
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    // Transmitting Send Actions triggered by client events on the Bluetooth Gatt Client


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        Log.d(GATT_CHARACTERISTIC_READ_TAG, String.valueOf(characteristic.getUuid()));

        // Write out all data in a generalized way.
        // If we are following a spec pertaining to a particular UUID we will need to parse it
        final byte[] data = characteristic.getValue();

        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);

            for(byte byteChar : data) {
                stringBuilder.append(String.format("%02X ", byteChar));
            }
            intent.putExtra(CHARACTERISTIC_KEY, new String(data) + "\n" +
                    stringBuilder.toString());
        }
        sendBroadcast(intent);
    }

    /**
     * @param assignedNumber Bluetooth LE Spec / Vendor assigned assignedNumber in Hex
     * @return A derived UUID from assignedNumber
     */
    public UUID convert(String assignedNumber) {
        long value = 0;
        try {
            value = Long.parseLong(assignedNumber, 16);
        } catch (NumberFormatException ignore) {
        }
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        return new UUID(MSB | (value << 32), LSB);
    }

    public void setMainCharacteristicNotifyEnabled(boolean enabled) {
        mMainCharacteristicNotifyEnabled = enabled;
    }

    private final BluetoothGattCallback mGattCallback;

    {
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                int newState) {
                String intentAction;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    intentAction = "action connected";
                    broadcastUpdate(intentAction);
                    Log.i(GATT_CLIENT_TAG, "Connected to GATT server.");
                    Log.i(GATT_CLIENT_TAG, "Attempting to start service discovery:" +
                            mBluetoothGatt.discoverServices());

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    intentAction = "action disconnected";
                    Log.i(GATT_CLIENT_TAG, "Disconnected from GATT server.");
                    broadcastUpdate(intentAction);
                }
            }

            @Override
            // New services discovered
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate("action services discovered");

                    // Set characteristic to notify, so we can get feedback in the client from the peripheral
                    BluetoothGattService service = gatt.getService(convert(MAIN_GATT_SERVICE_UUID));

                    if (service != null) {
                        BluetoothGattCharacteristic characteristic;
                        characteristic = service.getCharacteristic(convert(GATT_CHARACTERISTIC_POINT_VALUE_UUID));

                        if (characteristic != null) {
                            mBluetoothGatt.setCharacteristicNotification(characteristic,
                                    mMainCharacteristicNotifyEnabled);

                            // Will also need to set the descriptor on the Characteristic,
                            // or else the peripheral/server will not know to send out notifications
                            BluetoothGattDescriptor descriptor =
                                    characteristic.getDescriptor(convert(GATT_CHARACTERISTIC_CONFIG_UUID));

                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBluetoothGatt.writeDescriptor(descriptor); // This will trigger a callback to onDescriptorWrite
                            }
                        }
                    }
                } else {
                    Log.w(GATT_CLIENT_TAG, "onServicesDiscovered received: No Services Discovered");
                }
            }

            @Override
            // Result of a characteristic read operation
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(ACTION_DATA, characteristic);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattCharacteristic characteristic;
                    characteristic = gatt.getService(convert(MAIN_GATT_SERVICE_UUID))
                        .getCharacteristic(convert(GATT_CHARACTERISTIC_KICK_OFF_UUID));

                    if (characteristic != null) {
                        // Write a value to the characteristic to get the Server to start streaming the measured characteristic
                        // This value will be given by the spec
                        characteristic.setValue(new byte[]{1, 1});
                        mBluetoothGatt.writeCharacteristic(characteristic);

                        // All Done!
                    }
                }
            }

            @Override
            // Receive notifications if the descriptor was set to notify, for which the GATT Characteristic must also support
            // This will be done by the client
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                broadcastUpdate(ACTION_DATA, characteristic);
            }
        };
    }

    private BluetoothGatt mBluetoothGatt;
    private boolean mMainCharacteristicNotifyEnabled = true;

    public final static String ACTION_DATA = "action data";
    public final static String CHARACTERISTIC_KEY = "charKey";

    // Mock GATT Service
    private final static String MAIN_GATT_SERVICE_UUID = "123";
    // Mock GATT Characteristic per service
    private final static String GATT_CHARACTERISTIC_KICK_OFF_UUID = "1234"; // Properties: Write
    private final static String GATT_CHARACTERISTIC_CONFIG_UUID = "1235";  // Spec Client Characteristic Configuration
    private final static String GATT_CHARACTERISTIC_POINT_VALUE_UUID = "1236"; // The measurement, or data we are interested in reading/sensing

    private final static String GATT_CLIENT_TAG = "GATT_CALLBACK";
    private final static String GATT_CHARACTERISTIC_READ_TAG = "CHARACTERISTIC_READ";
}