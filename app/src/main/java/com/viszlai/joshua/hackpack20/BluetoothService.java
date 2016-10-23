package com.viszlai.joshua.hackpack20;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;

/**
 * Created by Joshua on 7/12/2015.
 */
public class BluetoothService {
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket mySocket = null;
    private BluetoothAdapter myBluetooth = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mmDevice = null;
    private boolean connected = false;
    private boolean connecting = true;
    private String inputStreamString = null;
    private boolean newMessage = false;
    private String readMessage = null;
    //Create handler to get messages from thread reading from input stream with arduino
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            newMessage = true;
            readMessage = (String)msg.obj;
        }
    };
    public boolean getNewMes(){
        return newMessage;
    }
    public void setNewMes(boolean set){
        newMessage = set;
    }
    public String getMessage(){
        return readMessage;
    }
    //Method to start communications with arduino in separate thread
    public void startConnectThread(String address){
        mmDevice = myBluetooth.getRemoteDevice(address);
        new ConnectThread().execute(mmDevice);
    }
    public boolean getConnected(){
        while (connecting == true){}
        return connected;
    }
    public void disconnect() {
            if (mySocket != null) {
                try{
                    mySocket.close();
                } catch (IOException e) {}
            }
    }
    public void writeBluetooth(String msg){
        if (mySocket != null){
            try{
                mySocket.getOutputStream().write(msg.toString().getBytes());
            } catch(IOException e){}
        }
    }

    //connect to arduino and start reading from input stream
    private class ConnectThread extends AsyncTask<BluetoothDevice, Void, Void> {
        protected Void doInBackground(BluetoothDevice... devices) {
            Log.d("getDeviceToPairWith", "Working[hopefully]");
            BluetoothSocket tmp = null;
            connecting = true;
            connected = false;
            try {
                tmp = devices[0].createRfcommSocketToServiceRecord(MY_UUID);
                mySocket = tmp;
                mySocket.connect();
                connected = true;
                beginRead();
                Log.d("connect", "Working[hopefully]");

            } catch (IOException e) {}
            connecting = false;
            return null;
        }
    }

    //read from input stream and send messages to handler
    private void beginRead() {
        Thread readThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        inputStreamString =
                                new Scanner(mySocket.getInputStream(), "UTF-8").
                                        useDelimiter("\n").next();
                        String status = new String(inputStreamString);
                        status = status.substring(0,status.length()-1);
                        Message msg = Message.obtain();
                        msg.obj = status;
                        msg.setTarget(mHandler);
                        msg.sendToTarget();
                    } catch (IOException e) {
                        Log.d("Error", "NoBueno");
                    } catch (NoSuchElementException e){}
                }
            }
        });
        readThread.start();
    }
}
