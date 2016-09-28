/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.starter;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.starter.DeviceSetupActivityFragment.FragmentSettings;

public class DeviceSetupActivity extends AppCompatActivity implements ServiceConnection, FragmentSettings {
    public final static String EXTRA_BT_DEVICE= "com.mbientlab.metawear.starter.DeviceSetupActivity.EXTRA_BT_DEVICE";

    public static class ReconnectDialogFragment extends DialogFragment implements  ServiceConnection {
        private static final String KEY_BLUETOOTH_DEVICE = "com.mbientlab.metawear.starter.DeviceSetupActivity.ReconnectDialogFragment.KEY_BLUETOOTH_DEVICE";

        private ProgressDialog reconnectDialog = null;
        private BluetoothDevice btDevice = null;
        private MetaWearBoard currentMwBoard = null;

        public static ReconnectDialogFragment newInstance(BluetoothDevice btDevice) {
            Bundle args = new Bundle();
            args.putParcelable(KEY_BLUETOOTH_DEVICE, btDevice);

            ReconnectDialogFragment newFragment = new ReconnectDialogFragment();
            newFragment.setArguments(args);

            return newFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            btDevice = getArguments().getParcelable(KEY_BLUETOOTH_DEVICE);
            getActivity().getApplicationContext().bindService(new Intent(getActivity(), MetaWearBleService.class), this, BIND_AUTO_CREATE);

            reconnectDialog = new ProgressDialog(getActivity());
            reconnectDialog.setTitle(getString(R.string.title_reconnect_attempt));
            reconnectDialog.setMessage(getString(R.string.message_wait));
            reconnectDialog.setCancelable(false);
            reconnectDialog.setCanceledOnTouchOutside(false);
            reconnectDialog.setIndeterminate(true);
            reconnectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    currentMwBoard.disconnect();
                    getActivity().finish();
                }
            });

            return reconnectDialog;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            currentMwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) { }
    }

    private BluetoothDevice btDevice;
    private MetaWearBoard mwBoard;

    private final String RECONNECT_DIALOG_TAG= "reconnect_dialog_tag";
    private final ConnectionStateHandler connectionHandler= new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            ((DialogFragment) getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG)).dismiss();
            ((DeviceSetupActivityFragment) getSupportFragmentManager().findFragmentById(R.id.device_setup_fragment)).reconnected();
        }

        @Override
        public void disconnected() {
            attemptReconnect();
        }

        @Override
        public void failure(int status, Throwable error) {
            Fragment reconnectFragment= getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG);
            if (reconnectFragment != null) {
                mwBoard.connect();
            } else {
                attemptReconnect();
            }
        }

        private void attemptReconnect() {
            ReconnectDialogFragment dialogFragment= ReconnectDialogFragment.newInstance(btDevice);
            dialogFragment.show(getSupportFragmentManager(), RECONNECT_DIALOG_TAG);
            mwBoard.connect();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_disconnect:
                mwBoard.setConnectionStateHandler(null);
                mwBoard.disconnect();
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        mwBoard.setConnectionStateHandler(null);
        mwBoard.disconnect();
        super.onBackPressed();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(connectionHandler);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public BluetoothDevice getBtDevice() {
        return btDevice;
    }
}
