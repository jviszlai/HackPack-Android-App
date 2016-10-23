package com.viszlai.joshua.hackpack20;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;


public class Bluetooth extends ActionBarActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    ListView devicelist;

    private BluetoothAdapter myBluetooth = BluetoothAdapter.getDefaultAdapter();
    public static String EXTRA_ADDRESS = "device_address";
    ArrayList myArrayAdapter = new ArrayList<String>();
    String address = null;
    /* BluetoothService is a separate service I created
     to allow communication through multiple activities
     */
    private BluetoothService myService = null;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        ((MyApplication)this.getApplication()).createService();
        myService = ((MyApplication)this.getApplication()).getService();
        devicelist = (ListView) findViewById(R.id.listView);
        //Check to see if bluetooth is supported on device
        if (myBluetooth == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not supported on this device.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            //Check to see if bluetooth is enabled, asks for permission to enable it
            if (!myBluetooth.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            pairedDevicesList();
        }



    }

    private void pairedDevicesList() {
        while (!myBluetooth.isEnabled()){}
        //Gets a list of all paired bluetooth devices
        Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
        String hackPackAddress = readFile("address_file.txt");
        connected = false;
        if (pairedDevices.size() > 0) {
            //Creates a graphic list for all the paired bluetooth devices
            for (BluetoothDevice device : pairedDevices) {
                /*checks if one of the devices matches with the address saved to internal storage
                (only if the app has been setup already)
                 */
                if (device.getAddress().equalsIgnoreCase(hackPackAddress)) {
                    address = hackPackAddress;
                    connectto();
                    //checks if HackPack is available to connect with
                    if (!connected){
                        finish();
                    } else{
                        /*
                        skips password input
                        (address can only be saved if password was previously entered correctly)
                         */
                        myService.writeBluetooth("S1");
                        skipPass();
                    }
                }
                myArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            //displays error if no paired Bluetooth devices exit
            Toast.makeText(getApplicationContext(), "No currently paired bluetooth devices", Toast.LENGTH_LONG).show();
        }
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, myArrayAdapter);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener);
    }



    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            //gets address of paired device that was selected and tries to connect to it
            String info = ((TextView) v).getText().toString();
            address = info.substring(info.length() - 17);
            connectto();
            if(!connected){
                finish();
            } else{
                //goes to password screen if connection successful
                runPassword();
            }
        }
    };
    //the method to skip password input if app is already setup
    public void skipPass(){
        Intent i = new Intent(Bluetooth.this, hpCommunicate.class);
        i.putExtra(EXTRA_ADDRESS, address);
        startActivity(i);
        finish();
    }
    //method to connect to HackPack
    public void connectto() {
        myService.startConnectThread(address);
        connected = myService.getConnected();
        if (connected) {
            Toast.makeText(getApplicationContext(), "Connected Successfully to HackPack", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "Connection Attempt to HackPack Unsuccessful", Toast.LENGTH_LONG).show();
        }

    }
    //Method to go to password screen
    public void runPassword(){
        Intent i = new Intent(Bluetooth.this, PasswordInput.class);
        i.putExtra(EXTRA_ADDRESS, address);
        startActivity(i);
        finish();
    }
    //Method to read internal storage file
    public String readFile(String fileName){
        String str = "";
        try {
            InputStream inputStream = openFileInput(fileName);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                str = stringBuilder.toString();
                return str;
            }
        } catch (IOException e) {return null;}
        return str;
    }

}