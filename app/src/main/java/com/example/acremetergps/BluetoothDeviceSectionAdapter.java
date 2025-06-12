package com.example.acremetergps;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.RequiresPermission;

import com.example.acremetergps.R;

import java.util.List;


public class BluetoothDeviceSectionAdapter extends BaseAdapter {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_DEVICE = 1;

    private final Context context;
    private final List<BluetoothDevice> pairedDevices;

    public BluetoothDeviceSectionAdapter(Context context, List<BluetoothDevice> pairedDevices) {
        this.context = context;
        this.pairedDevices = pairedDevices;
    }

    @Override
    public int getCount() {
        return pairedDevices.size();
    }


    @Override
    public Object getItem(int position) {
        return pairedDevices.get(position);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_DEVICE;
    }


    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_bluetooth_device, parent, false);
        }
        BluetoothDevice device = (BluetoothDevice) getItem(position);
        TextView textName = convertView.findViewById(R.id.deviceName);
        TextView textAddress = convertView.findViewById(R.id.deviceAddress);

        textName.setText(device.getName() != null ? device.getName() : "Unknown");
        textAddress.setText(device.getAddress());

        return convertView;
    }
}
