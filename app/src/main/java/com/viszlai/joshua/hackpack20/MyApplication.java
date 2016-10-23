package com.viszlai.joshua.hackpack20;

import android.app.Application;

/**
 * Created by Joshua on 7/13/2015.
 */
public class MyApplication extends Application {
    private BluetoothService myService;
    public void createService(){
        myService = new BluetoothService();
    }
    public BluetoothService getService(){
        return myService;
    }
    public void removeService(){
        myService = null;
    }
}
