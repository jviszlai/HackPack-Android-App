package com.viszlai.joshua.hackpack20;

import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Created by Joshua on 7/6/2015.
 */
public class hpCommunicate extends ActionBarActivity{

    private String address = null;
    private FileOutputStream outputStream;
    private String status;
    private String readmessage;
    private BluetoothService myService = null;

    //handle messages from thread reading from input stream with arduino
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            readmessage = (String)msg.obj;
            changeText(readmessage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hp_communicate);
        Intent newint = getIntent();
        address = newint.getStringExtra(Bluetooth.EXTRA_ADDRESS);
        try{
            outputStream = openFileOutput("address_file.txt", this.MODE_PRIVATE);
            outputStream.write(address.getBytes());
            outputStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        myService = ((MyApplication)this.getApplication()).getService();
        beginRead();

        //display locked or unlocked based on arduino status
        try {
            InputStream inputStream = openFileInput("status_file.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                status = stringBuilder.toString();
                changeText(status);
            }
        }
        catch(IOException e){}

        Button buttonLock = (Button) findViewById(R.id.button);
        buttonLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockPack();
            }
        });
        Button buttonUnlock=(Button) findViewById(R.id.button2);
        buttonUnlock.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                unlockPack();
            }

        });
        Button buttonRemoveMem = (Button) findViewById(R.id.button3);
        buttonRemoveMem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeMemory();
            }

        });

    }


    @Override
    protected void onPause(){
        super.onPause();
        myService.writeBluetooth("E1");
        myService.disconnect();
        ((MyApplication)this.getApplication()).removeService();
        finish();
    }


    private void lockPack(){
        myService.writeBluetooth("lock");
    }

    private void unlockPack(){
        myService.writeBluetooth("unlock");
    }

    //remove address and arduino status from memory to allow new HackPack connection
    private void removeMemory(){
        hpCommunicate.this.deleteFile("address_file.txt");
        hpCommunicate.this.deleteFile("status_file.txt");
        finish();
    }
    private void changeText(String stat){
        TextView text = (TextView)findViewById(R.id.textView3);
        if (stat.equalsIgnoreCase("lock")){
            text.setText("Locked");
        }
        else if (stat.equalsIgnoreCase("unlock")){
            text.setText("Unlocked");
        }
        writeFile("status_file.txt", stat);
    }


    //read from arduino and send messages to handler
    private void beginRead() {
        Thread readThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    if (myService.getNewMes()){
                        Message msg = Message.obtain();
                        msg.obj = myService.getMessage();
                        msg.setTarget(mHandler);
                        msg.sendToTarget();
                        myService.setNewMes(false);
                    }
                }
            }
        });
        readThread.start();
    }

    public void writeFile(String fileName, String data){
        try{
            outputStream = openFileOutput(fileName, this.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}


