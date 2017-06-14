package com.lxx.gprint.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lxx.gprint.R;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by android on 2017/6/13.
 */

public class BluetoothDeviceAdapter extends BaseAdapter {

    private Context mContext;
    public List<BluetoothDevice> mData;


    public BluetoothDeviceAdapter(Context mContext, List<BluetoothDevice> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }


    @Override
    public int getCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        final BluetoothDevice device = getItem(position);
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.item_bluetooth_device_view, null);
            holder.btn = (TextView) convertView.findViewById(R.id.btn);
            holder.textView = (TextView) convertView.findViewById(R.id.tv);
            holder.textStatus = (TextView) convertView.findViewById(R.id.tv_status);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        holder.textView.setText(device.getName() + "--type=" + device.getType());

        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            holder.textStatus.setText("已派对");
            holder.btn.setVisibility(View.VISIBLE);
        } else {
            holder.textStatus.setText("");
            holder.btn.setVisibility(View.INVISIBLE);
        }


        holder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //取消配对
                    try {
                        Method removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
                        removeBondMethod.invoke(device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        return convertView;
    }

    static class ViewHolder {
        TextView btn;
        TextView textView;
        TextView textStatus;
    }

}
