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

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Bmi160Gyro;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class DeviceSetupActivityFragment extends Fragment implements ServiceConnection {
    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }

    private static final String TAG = "DeviceActivity";
    private MetaWearBoard mwBoard= null;
    private FragmentSettings settings;
    private Bmi160Accelerometer accModule;
    private Bmi160Gyro gyroModule;
    private ArrayList<SensorRecord> mAccelRecords = new ArrayList<>();
    private ArrayList<SensorRecord> mGyroRecords = new ArrayList<>();
    private String label;
    private Button mStartButton;
    private Button mStopButton;
    private Button mSaveDataButton;

    public DeviceSetupActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity owner= getActivity();
        if (!(owner instanceof FragmentSettings)) {
            throw new ClassCastException("Owning activity must implement the FragmentSettings interface");
        }

        settings= (FragmentSettings) owner;
        owner.getApplicationContext().bindService(new Intent(owner, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getActivity().getApplicationContext().unbindService(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_device_setup, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mStartButton = (Button)view.findViewById(R.id.acc_start);
        mStopButton = (Button)view.findViewById(R.id.acc_stop);
        mSaveDataButton = (Button)view.findViewById(R.id.button_save_data);

        /**
         * Start accel/gyro sapmling. Add each record from stream to ArrayList of SensorRecords.
         */
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accModule.routeData().fromAxes().stream("acc_stream").commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.subscribe("acc_stream", new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message msg) {
                                        Log.i(TAG, msg.getData(CartesianFloat.class).toString());
                                        mAccelRecords.add(new SensorRecord(msg.getTimestampAsString(),
                                                msg.getData(CartesianFloat.class)));
                                    }
                                });
                                accModule.setOutputDataRate(25.0f);
                                accModule.enableAxisSampling();
                                accModule.start();
                            }
                        });
                gyroModule.routeData().fromAxes().stream("gyro_stream").commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.subscribe("gyro_stream", new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message msg) {
                                        Log.i(TAG, msg.getData(CartesianFloat.class).toString());
                                        mGyroRecords.add(new SensorRecord(msg.getTimestampAsString(),
                                                msg.getData(CartesianFloat.class)));
                                    }
                                });
                                gyroModule.setOutputDataRate(25.0f);
                                gyroModule.start();
                            }
                        });
                mStartButton.setEnabled(false);
                mStopButton.setEnabled(true);
            }
        });

        /**
         * Stop sampling and remove accel/gyro routes.
         */
        view.findViewById(R.id.acc_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accModule.stop();
                accModule.disableAxisSampling();
                gyroModule.stop();
                mwBoard.removeRoutes();
                mStopButton.setEnabled(false);
                mSaveDataButton.setEnabled(true);
            }
        });

        /**
         * Save data to shared local storage.
         */
        view.findViewById(R.id.button_save_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                labelPrompt();
                mSaveDataButton.setEnabled(false);
                mStartButton.setEnabled(true);
            }
        });
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(settings.getBtDevice());
        ready();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() { }

    /**
     * Called when the mwBoard field is ready to be used
     */
    public void ready() {
        try {
            accModule = mwBoard.getModule(Bmi160Accelerometer.class);
            gyroModule = mwBoard.getModule(Bmi160Gyro.class);
        }
        catch (UnsupportedModuleException e) {
            Toast.makeText(getContext(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create necessary directories and write files.
     * WARNING: This will not work if you set Target API to 23 and above.
     * See https://stackoverflow.com/questions/32635704/android-permission-doesnt-work-even-if-i-have-declared-it
     * for more info.
     */
    public void writeFiles() {
        // Create directory and file
        File directory = new File(Environment.getExternalStorageDirectory()
                + File.separator + "WISDM");
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }

        // Write accel logs to file
        File accelFile = new File(directory, label + "_"
                + mAccelRecords.get(0).getTimestamp()
                + "_accel.txt");

        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(accelFile));
            for (SensorRecord record : mAccelRecords) {
                bufferedWriter.write(record.toString());
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        }
        catch (IOException e) {
            Log.e(TAG, "Write failed.");
            e.printStackTrace();
        }

        // Write gyro logs to file
        File gyroFile = new File(directory, label + "_"
                + mGyroRecords.get(0).getTimestamp()
                + "_gyro.txt");

        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(gyroFile));
            for (SensorRecord record : mGyroRecords) {
                bufferedWriter.write(record.toString());
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        }
        catch (IOException e) {
            Log.e(TAG, "Write failed.");
            e.printStackTrace();
        }

        // 'Discover' new files
        String[] path = {accelFile.getAbsolutePath(), gyroFile.getAbsolutePath()};
        MediaScannerConnection.scanFile(getContext(), path, null, null);
    }

    /**
     * Prompt user for file label.
     */
    public void labelPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        builder.setTitle(getString(R.string.label_prompt));

        final EditText input = new EditText(getActivity());
        builder.setView(input);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                label = input.getText().toString().trim().toLowerCase().replace(" ", "_");
                writeFiles();
            }
        });
        builder.show();
    }
}
