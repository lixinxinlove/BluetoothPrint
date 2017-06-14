package com.lxx.gprint;

import android.bluetooth.BluetoothAdapter;
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
import android.widget.Toast;

import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.io.GpDevice;
import com.gprinter.service.GpPrintService;

import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private GpService mGpService = null;
    private PrinterServiceConnection conn = null;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();

        Log.e(TAG, "onCreate");
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE); // bindService
        registerBroadcast();
    }

    private void initData() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

                } else if (type == GpDevice.STATE_NONE) {

                } else if (type == GpDevice.STATE_VALID_PRINTER) {

                } else if (type == GpDevice.STATE_INVALID_PRINTER) {

                }
            }
        }
    };


    int printerConnectStatus = 0;


    /**
     * 开启蓝牙
     *
     * @param view
     */
    public void openBluetooth(View view) {
        // getPrinterConnectStatus(0)

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(this, BluetoothListActivity.class);
            startActivity(intent);
        } else {
            try {
                printerConnectStatus = mGpService.getPrinterConnectStatus(0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }


            Log.e(TAG,"printerConnectStatus="+printerConnectStatus);

            if (printerConnectStatus != GpDevice. STATE_CONNECTED) {
                Intent intent = new Intent(this, BluetoothListActivity.class);
                startActivity(intent);
            }
        }
    }

    public void testPrint(View view) {
        sendReceipt3();
    }

    public void testPrintImg(View view) {
        sendReceiptBmp(1);
    }


    public void closePortPrint(View view){
        try {
            mGpService.closePort(0);
        } catch (RemoteException e) {

        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(PrinterStatusBroadcastReceiver);
        if (conn != null) {
            unbindService(conn); // unBindService
        }
    }

    /**
     * 打印文字
     */
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
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);

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
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
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

    /**
     * 条形码
     *
     * @param v
     */
    public void printLine(View v) {
        EscCommand esc = new EscCommand();
        esc.addPrintAndFeedLines((byte) 3);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
        // 设置条码高度为 60 点
        esc.addSetBarcodeHeight((byte) 100);
        // 设置条码单元宽度为 1 点
        esc.addSetBarcodeWidth((byte) 2);
        // 打印 Code128 码
        esc.addCODE128(esc.genCodeB("11001100"));
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
