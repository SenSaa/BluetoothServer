package com.example.yusuf.bluetoothserver;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {

    BluetoothAdapter bluetoothAdapter;
    ListView listView;
    ArrayAdapter<String> arrayAdapter;

    static final int Permission_Req_Code = 1;
    Button pairBtn;
    Button discoverBtn;
    ToggleButton discoverToggle;
    boolean isRegistered;
    boolean btExitSetting;
    boolean inSettings;
    boolean serverListening;

    RadioGroup radioGroup;
    RadioButton receiveRadioBtn;
    RadioButton sendRadioBtn;

    EditText userInputField;
    String userInput = "";
    String clientMsg;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Log.v("UUID",String.valueOf(UUID.randomUUID()));

        /* ___ Setting Up Bluetooth ___ */
        // - Get the BluetoothAdapter -
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ////BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        ////bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast noBT_toast = Toast.makeText(this,"Device does not support Bluetooth!",Toast.LENGTH_SHORT);
            noBT_toast.show();
        }

        // - Enable Bluetooth -
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        }


        listView = (ListView)findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        listView.setAdapter(arrayAdapter);


        // * As of Marshmallow, permissions that are not considered "normal" (dangerous - may expose user data), must be granted by user at runtime.
        // This includes starting bluetooth discovery feature.

        // Request Location Permission (needed for Bluetooth) at RunTime:
        requestPermission();


        pairBtn = (Button)findViewById(R.id.pairBtn);
        pairBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ___ Finding paired devices ___ //
                pairedDevices();
            }
        });

        discoverToggle = (ToggleButton)findViewById(R.id.discoverToggle);
        discoverToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    discoverDevices();
                }
                else if (!isChecked) {
                    disableDiscovery();
                }
            }
        });


        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        receiveRadioBtn = (RadioButton) findViewById(R.id.receiveRadioBtn);
        sendRadioBtn = (RadioButton) findViewById(R.id.sendRadioBtn);

        userInputField = (EditText) findViewById(R.id.userInputField);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_main, menu);

        View view = menu.findItem(R.id.action_connect).getActionView();
        Switch connect_switch = (Switch) view.findViewById(R.id.connect_switch);
        connect_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AcceptThread acceptThread = new AcceptThread();
                if (isChecked) {
                    Log.d("Switch", "Checked");
                        acceptThread.start();
                }
                else if (!isChecked) {
                    Log.d("Switch", "!Checked");
                    acceptThread.closeConnection();
                }
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.action_settings) {
            Intent settingIntent = new Intent(this, Settings.class);
            startActivity(settingIntent);

            inSettings = true;
        }

        if (item.getItemId() == R.id.action_exit) {
            onStop();
            finish();
        }

        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().getExtras() != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            btExitSetting = sharedPref.getBoolean("pref_key_exit_bluetooth", false);
            Log.v("Bundle", String.valueOf(btExitSetting));

            inSettings = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w("onStop", "invoked!");

        disableDiscovery();

        if (btExitSetting && !inSettings) {
            bluetoothAdapter.disable();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w("onDestroy", "invoked!");
    }


    /* ___ Finding paired devices ___ */
    private void pairedDevices() {
        // - Querying paired devices -
        final Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                arrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }


    // - Discovering devices -
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v("onReceive","Invoked");

            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                Log.v("BT Devices","Found");

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                arrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.e("BT", device.getName() + "\n" + device.getAddress());
                arrayAdapter.notifyDataSetChanged();
            }

            else {

                Log.v("BT Devices","!!!Found");

            }

            isRegistered = true;

        }
    };


    private void discoverDevices() {
        // - Enabling discoverability -
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        // * Add following actions to ensure the start of the BT dicovery mode.
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(broadcastReceiver, filter); // Don't forget to unregister during onDestroy

        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        if (bluetoothAdapter != null) {
            Log.e("adapter","Not Null!");
        }

        bluetoothAdapter.startDiscovery();
        if (bluetoothAdapter.isDiscovering()) {
            Log.e("BTadapter","Discovering!");
        }
    }

    private void disableDiscovery() {
        bluetoothAdapter.cancelDiscovery();
        if (isRegistered) {
            unregisterReceiver(broadcastReceiver);
        }
    }


    /* ___ Accepting Client Connection. ___*/
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;
        ConnectedThread connectedThread;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                String NAME = "BluetoothServer";
                UUID MY_UUID = UUID.fromString("0ae3d869-470e-45d1-b147-a10882fc1bd2");
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);

                Log.e("Server", "Listening");

            }
            catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();

                    Log.e("Connection","Accepted");

                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {

                    // Do work to manage the connection (in a separate thread)
                    connectedThread = new ConnectedThread(socket);
                    connectedThread.start();

                    cancel();

                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }

        public void closeConnection() {
            if (connectedThread != null) {
                connectedThread.close();
            }
        }

    }


    /* Managing Connection */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        BufferedReader inputStream;
        PrintWriter outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();

                inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = new PrintWriter(socket.getOutputStream(), true);


            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.receiveRadioBtn) {
                        Log.d("RadioButton", "Receive");

                        read();

                        TextView clientMsgTxV = (TextView) findViewById(R.id.clientMsgTxV);
                        clientMsgTxV.setText(clientMsg);
                        Log.i("Server MSG", clientMsg);
                    }
                    else if (checkedId == R.id.sendRadioBtn) {
                        Log.d("RadioButton", "Send");
                        userInput = userInputField.getText().toString();
                        Log.e("RadioButton", "Sent!!!");

                        write();
                    }
                }
            });
        }

        private void read() {
            try {
                clientMsg = inputStream.readLine();
                Log.v("InputStream","Read");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void write() {
            // To ensure that the User Input is added to the output stream - add a conditional statement to check if userInput variable is not null.
            if (userInput != null) {
                Log.e("User Input", userInput);
                outputStream.println(userInput);
                Log.d("Output Stream", "Written to Client");
            }
        }

        /* Shutdown the connection */
        public void close() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    private void requestPermission() {
        // Here, this is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        Permission_Req_Code);

                // Permission_Req_Code is an app-defined int constant.
                // The callback method gets the result of the request.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
    String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Permission_Req_Code: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted - Do the BT-related task you need to do.

                } else {

                    // permission denied - Disable the functionality that depends on this permission.

                    String btPermissionDeniedStr = "Sorry, Bluetooth communication is not possible without granting location permission";
                    Toast.makeText(this,btPermissionDeniedStr,Toast.LENGTH_LONG).show();

                }
                return;
            }
        }
    }



}
