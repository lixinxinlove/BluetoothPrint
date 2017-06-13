package com.lxx.gprint.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lxx.gprint.R;

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
        BluetoothDevice deview = getItem(position);
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.item_bluetooth_device_view, null);
            holder.textView = (TextView) convertView.findViewById(R.id.tv);
            holder.textStatus = (TextView) convertView.findViewById(R.id.tv_status);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.textView.setText(deview.getName());

        if (deview.getBondState() == BluetoothDevice.BOND_BONDED) {
            holder.textStatus.setText("已派对");
        } else {
            holder.textStatus.setText("");
        }


        return convertView;
    }

    static class ViewHolder {
        TextView textView;
        TextView textStatus;
    }

}
