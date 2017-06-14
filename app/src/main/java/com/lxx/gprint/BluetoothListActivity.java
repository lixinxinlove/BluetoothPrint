package com.lxx.gprint;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.gprinter.aidl.GpService;
import com.gprinter.command.GpCom;
import com.gprinter.io.GpDevice;
import com.gprinter.service.GpPrintService;
import com.lxx.gprint.adapter.BluetoothDeviceAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static android.util.Log.e;

public class BluetoothListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_ENABLE_BT = 111;
    private static final String TAG = "BluetoothListActivity";

    private ListView listView;
    private GpService mGpService = null;
    private PrinterServiceConnection conn = null;


    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;   //已经配对的蓝牙设备
    private BluetoothDeviceAdapter adapter;
    private List<BluetoothDevice> mData;

    private String MAC = "";   //蓝牙地址

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_list);
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE); // bindService

        initView();
        initData();

        registerBroadcast();
        registerBluetoothBroadcast();

        if (Build.VERSION.SDK_INT >= 23) {
            requestBluetoothPermission();
        }
    }

    private BroadcastReceiver PrinterStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GpCom.ACTION_CONNECT_STATUS.equals(intent.getAction())) {
                int type = intent.getIntExtra(GpPrintService.CONNECT_STATUS, 0);
                int id = intent.getIntExtra(GpPrintService.PRINTER_ID, 0);
                e(TAG, "返回设备");
                e(TAG, "返回设备" + "type=" + type + "---id=" + id);
                if (type == GpDevice.STATE_CONNECTING) {
                    e(TAG, "正在连接 ");
                    Toast.makeText(BluetoothListActivity.this, "正在连接", Toast.LENGTH_SHORT).show();
                } else if (type == GpDevice.STATE_NONE) {
                    e(TAG, "连接断开");
                    Toast.makeText(BluetoothListActivity.this, "连接断开", Toast.LENGTH_LONG).show();
                } else if (type == GpDevice.STATE_VALID_PRINTER) {
                    e(TAG, "有效设备");
                    Toast.makeText(BluetoothListActivity.this, "有效设备--连接成功", Toast.LENGTH_LONG).show();
                } else if (type == GpDevice.STATE_INVALID_PRINTER) {
                    e(TAG, "无效设备");
                    Toast.makeText(BluetoothListActivity.this, "无效设备", Toast.LENGTH_LONG).show();
                } else if (type == GpDevice.STATE_CONNECTED) {
                    e(TAG, "已经连接");
                    Toast.makeText(BluetoothListActivity.this, "已经连接", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(GpCom.ACTION_CONNECT_STATUS);
        this.registerReceiver(PrinterStatusBroadcastReceiver, filter);
        e(TAG, "registerBroadcast");
    }

    private void initData() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            mData.addAll(pairedDevices);
            adapter.notifyDataSetChanged();
        }
    }

    private void initView() {
        listView = (ListView) findViewById(R.id.list_view);
        mData = new ArrayList<>();
        adapter = new BluetoothDeviceAdapter(this, mData);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            e(TAG, "onServiceDisconnected() called");
            mGpService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGpService = GpService.Stub.asInterface(service);
            e(TAG, "绑定服务");
        }
    }


    /**
     * 监听蓝牙设备
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView

                if (isNotNull(device.getName())) {
                    mData.add(device);
                }
            }
            adapter.mData = mData;
            adapter.notifyDataSetChanged();
        }
    };

    // Register the BroadcastReceiver
    private void registerBluetoothBroadcast() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }


    public static boolean isNotNull(String str) {
        if (null == str || "".equals(str.trim()) || "null".equals(str.trim())) {
            return false;
        }
        return true;
    }


    /**
     * 开启蓝牙
     *
     * @param view
     */
    public void openBluetooth(View view) {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * 扫描蓝牙设备
     *
     * @param view
     */
    public void startDiscovery(View view) {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.startDiscovery();
            mData.clear();
        } else {
            Toast.makeText(this, "请手动打开蓝牙", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "蓝牙开启", Toast.LENGTH_SHORT).show();

        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "蓝牙失败", Toast.LENGTH_SHORT);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        mBluetoothAdapter.cancelDiscovery();

        if (adapter.getItem(position).getBondState() == BluetoothDevice.BOND_BONDED) {
            MAC = adapter.getItem(position).getAddress();
            try {
                mGpService.openPort(0, 4, MAC, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            BluetoothDevice device = adapter.getItem(position);
            MAC = device.getAddress();
//            byte[] pin = "0000".getBytes();
//            device.setPin(pin);
//            //取消用户输入
//            try {
//                Method cancelPairingUserInputMethod = BluetoothDevice.class.getMethod("cancelPairingUserInput");
//                cancelPairingUserInputMethod.invoke(device);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            try {
                mGpService.openPort(0, 4, MAC, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            //  boolean f = device.createBond();

//            if (f) {
//                Toast.makeText(this, "派对成功", Toast.LENGTH_LONG).show();
//
//                listView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            mGpService.openPort(0, 4, MAC, 0);
//                        } catch (RemoteException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });

            // } else {
            //  Toast.makeText(this, "派对失败", Toast.LENGTH_LONG).show();
            //  }
        }
    }

    public void requestBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH)) {
                Toast.makeText(this, "蓝牙", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 100);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 100);
            }
        } else {
            // call();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 100: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //  call();
                } else {
                    Toast.makeText(this, "没有Permissions", Toast.LENGTH_SHORT).show();
                    //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 100);
                }
                return;
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mReceiver);
        this.unregisterReceiver(PrinterStatusBroadcastReceiver);
    }
}
