package com.example.john.mybluetoothbledemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BlueToothLE_Activity extends AppCompatActivity {
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    mBluetoothReady = true;
                    mBluetoothStatusView.setText(getString(R.string.bluetooth_le_ready));
                    Toast.makeText(this,getString(R.string.bluetooth_le_ready), Toast.LENGTH_SHORT).show();
                } else {
                    mBluetoothReady = false;
                    mBluetoothStatusView.setText(R.string.bluetooth_le_detected);
                    Toast.makeText(this,getString(R.string.bluetooth_le_not_ready), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_le_activity);
        mBluetoothStatusView = findViewById(R.id.blueToothStatus);

        mRecyclerAdapter = new MyRecyclerViewAdapter();
        if (savedInstanceState != null) {
            mRecyclerAdapter.restore(savedInstanceState);
        }

        final RecyclerView recyclerView = findViewById(R.id.listDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mRecyclerAdapter);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mScanHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        } else {
            mLocationGranted = true;
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothReady = true;
            mBluetoothStatusView.setText(getString(R.string.bluetooth_le_ready));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                    mLocationGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mRecyclerAdapter.save(outState);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.

            mScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback2);
                    } else {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.getBluetoothLeScanner().startScan(getScanFilters(), getScanSettings(), mLeScanCallback2);
            } else {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        } else {
            mScanning = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback2);
            } else {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private List<ScanFilter> getScanFilters() {
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter main = new ScanFilter.Builder()
                .build();
        filters.add(main);
        return filters;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanSettings getScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // Matches all, without an active filter
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE) // Matches devices with a higher threshold of signal strength
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT); // Matches a few number of advertisements per filter
        }
        return builder.build();
    }

    public void ClickScan(View view) {
        if (mBluetoothReady && mLocationGranted) {
            if (!mScanning) {
                mRecyclerAdapter.clearDevices();
                scanLeDevice(true);
            }
        } else {
            Toast.makeText(this,getString(R.string.bluetooth_le_recommendation), Toast.LENGTH_SHORT).show();
        }
    }

    /** Adapter to the recycler view. It holds strong reference to the data
     *
     */
    private static class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.DeviceViewHolder> {
        /**
         * @param info the info of the device
         * @return true if the info was successfully added and info is unique
         */
        void addDeviceInfo(String info) {
            mDevices.add(info);
        }

        void clearDevices() {
            mDevices.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item, parent, false);
            TextView textView = view.findViewById(R.id.text1);
            return new DeviceViewHolder(view, textView);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            holder.mText.setText(mDevices.get(position));
        }

        @Override
        public int getItemCount() {
            return mDevices.size();
        }

        void save(Bundle out) {
            out.putStringArrayList("devices", mDevices);
        }

        void restore(Bundle in) {
            ArrayList<String> items = in.getStringArrayList("devices");

            if (items != null) {
                mDevices = items;
            }
        }

        static class DeviceViewHolder extends RecyclerView.ViewHolder {
            DeviceViewHolder(View itemView, TextView text) {
                super(itemView);
                mText = text;
            }

            TextView mText;
        }

        private ArrayList<String> mDevices = new ArrayList<>();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mRecyclerAdapter.addDeviceInfo(device.toString() + " rssi="+rssi+" record length="+scanRecord.length);
                            mRecyclerAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback mLeScanCallback2 = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d("ScanZZZ", "scan result");
            mRecyclerAdapter.addDeviceInfo(result.toString());
            mRecyclerAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d("ScanZZZ", "scan results");
            if (results != null) {
                for (ScanResult result : results) {
                    mRecyclerAdapter.addDeviceInfo(result.toString());
                }
                mRecyclerAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("ScanZZZ", "scan failed");
        }
    };

    private BluetoothAdapter mBluetoothAdapter;
    private TextView mBluetoothStatusView;
    private MyRecyclerViewAdapter mRecyclerAdapter;
    private boolean mBluetoothReady = false;
    private boolean mLocationGranted = false;
    private boolean mScanning;
    private Handler mScanHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_LOCATION = 2;
}