package com.lxx.gprint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.io.GpDevice;
import com.gprinter.service.GpPrintService;
import com.lxx.gprint.adapter.BluetoothDeviceAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_ENABLE_BT = 100;
    private final String TAG = "MainActivity";
    private ListView listView;
    private GpService mGpService = null;
    private PrinterServiceConnection conn = null;


    private String MAC = "";   //蓝牙地址

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;   //已经配对的蓝牙设备
    private BluetoothDeviceAdapter adapter;
    private List<BluetoothDevice> mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();

        Log.e(TAG, "onCreate");
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE); // bindService
        registerBroadcast();
        registerBluetoothBroadcast();
    }

    private void initData() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            mData.addAll(pairedDevices);
        }

    }

    private void initView() {
        listView = (ListView) findViewById(R.id.list_view);
        mData = new ArrayList<>();
        adapter = new BluetoothDeviceAdapter(this, mData);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (adapter.getItem(position).getBondState() == BluetoothDevice.BOND_BONDED) {
            MAC = adapter.getItem(position).getAddress();
        } else {
            boolean f = adapter.getItem(position).createBond();
            if (f) {
                Toast.makeText(this, "派对成功", Toast.LENGTH_LONG).show();
                MAC = adapter.getItem(position).getAddress();
            } else {
                Toast.makeText(this, "派对失败", Toast.LENGTH_LONG).show();
            }
        }

    }

    class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected() called");
            mGpService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGpService = GpService.Stub.asInterface(service);
            Log.e(TAG, "绑定服务");
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
                mData.add(device);
            }

            adapter.mData = mData;
            adapter.notifyDataSetChanged();
        }
    };
    // Register the BroadcastReceiver

    private void registerBluetoothBroadcast() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        // Don't forget to unregister during onDestroy
    }


    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(GpCom.ACTION_CONNECT_STATUS);
        this.registerReceiver(PrinterStatusBroadcastReceiver, filter);
        Log.e(TAG, "registerBroadcast");
    }

    private BroadcastReceiver PrinterStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GpCom.ACTION_CONNECT_STATUS.equals(intent.getAction())) {
                int type = intent.getIntExtra(GpPrintService.CONNECT_STATUS, 0);
                int id = intent.getIntExtra(GpPrintService.PRINTER_ID, 0);

                Log.e(TAG, "返回设备");
                Log.e(TAG, "返回设备" + "type=" + type + "---id=" + id);

                if (type == GpDevice.STATE_CONNECTING) {
                    //    setProgressBarIndeterminateVisibility(true);
                } else if (type == GpDevice.STATE_NONE) {
                    ///   setProgressBarIndeterminateVisibility(false);

                } else if (type == GpDevice.STATE_VALID_PRINTER) {
                    //   setProgressBarIndeterminateVisibility(false);

                } else if (type == GpDevice.STATE_INVALID_PRINTER) {
                    //   setProgressBarIndeterminateVisibility(false);
                }
            }
        }
    };


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
        }
    }


    public void connPrint(View view) {
        try {
            mGpService.openPort(0, 4, MAC, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void testPrint(View view) {
        sendReceipt3();
    }

    public void testPrintImg(View view) {
        sendReceiptBmp(1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "蓝牙开启", Toast.LENGTH_SHORT);

        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "蓝牙失败", Toast.LENGTH_SHORT);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(PrinterStatusBroadcastReceiver);
        this.unregisterReceiver(mReceiver);
        if (conn != null) {
            unbindService(conn); // unBindService
        }
    }


    void sendReceipt3() {
        EscCommand esc = new EscCommand();

        esc.addPrintQRCode();
        esc.addText("0000000000000000000\n1234567890\n123455333\n0000000000000000000"); // 打印文字
        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rel;
        try {
            rel = mGpService.sendEscCommand(0, str);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    void sendReceiptBmp(int i) {
        EscCommand esc = new EscCommand();
        /* 打印图片 */
        esc.addText("Print bitmap!\n"); // 打印文字
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        esc.addRastBitImage(b, 384, 0); // 打印图片
        esc.addText("第 " + i + " 份\n"); // 打印文字

        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rel;
        try {
            rel = mGpService.sendEscCommand(0, str);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void sendNoBug(View v) {

        EscCommand esc = new EscCommand();

        esc.addText("Print QRcode\n"); // 打印文字
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31); // 设置纠错等级
        esc.addSelectSizeOfModuleForQRCode((byte) 8);// 设置qrcode模块大小
        esc.addStoreQRCodeData("lixinxin");// 设置qrcode内容
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();

        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rel;
        try {
            rel = mGpService.sendEscCommand(0, str);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }





}
