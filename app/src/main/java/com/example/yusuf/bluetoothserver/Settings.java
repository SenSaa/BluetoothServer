package com.example.yusuf.bluetoothserver;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;


public class Settings extends Activity /* implements CompoundButton.OnCheckedChangeListener */ {

    CheckBox exitBtCheckBox;
    Switch exitBTSwitch;
    boolean disableBluetoothOnExit;
    Intent mainIntent;
    String bluetoothExitState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsPreferences()).commit();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.w("Back Button","Pressed");

        // Update
        mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra(bluetoothExitState, disableBluetoothOnExit);
        startActivity(mainIntent);
    }


    public class SettingsPreferences extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);
        }

    }

}
