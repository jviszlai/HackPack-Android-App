package com.viszlai.joshua.hackpack20;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class PasswordInput extends ActionBarActivity {
    private BluetoothService myService;
    private String pass;
    private EditText input;
    private String readmessage;
    private String address;
    public static String EXTRA_ADDRESS = "device_address";

    //handle messages from thread reading input stream with arduino
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            readmessage = (String)msg.obj;
            if (readmessage.equalsIgnoreCase("C1")){
                Intent i = new Intent(PasswordInput.this, hpCommunicate.class);
                i.putExtra(EXTRA_ADDRESS, address);
                startActivity(i);
                finish();
            }
            else if(readmessage.equalsIgnoreCase("C0")){
                Toast.makeText(getApplicationContext(), "Incorrect password, try again.", Toast.LENGTH_LONG).show();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_input);
        Intent newint = getIntent();
        address = newint.getStringExtra(Bluetooth.EXTRA_ADDRESS);
        myService = ((MyApplication)this.getApplication()).getService();
        if (myService.getConnected()){
            beginRead();
        }
        Button sendButton = (Button) findViewById(R.id.sendPassB);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPass();
            }
        });
    }

    //send password to arduino so it can compare to password set on device
    public void sendPass(){
        input = (EditText)findViewById(R.id.passInput);
        pass = input.getText().toString();
        pass = md5(pass);
        Log.d("SENDINGPASS", pass);
        myService.writeBluetooth("S0");
        myService.writeBluetooth(pass);

    }

    //convert password to md5 for simple layer of security
    public String md5(final String password) {
        try {

            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(password.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    //read from arduino and send messages to handler
    void beginRead() {
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
}
