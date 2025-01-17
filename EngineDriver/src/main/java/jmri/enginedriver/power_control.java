/* Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jmri.enginedriver;

import static android.view.InputDevice.getDevice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class power_control extends AppCompatActivity {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Drawable power_on_drawable;  //hold background graphics for power button
    private Drawable power_off_drawable;
    private Drawable power_unknown_drawable;
    private Menu PMenu;

    private Toolbar toolbar;

    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class power_control_handler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE:
                    String response_str = msg.obj.toString();
                    if (response_str.length() >= 3 && response_str.substring(0, 3).equals("PPA")) {  //refresh power state
                        refresh_power_control_view();
                    }
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refresh_power_control_view();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
            }
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    public class button_listener implements View.OnClickListener {

        public void onClick(View v) {
            int newState = 1;
            if (mainapp.power_state.equals("1")) { //toggle to opposite value 0=off, 1=on
                newState = 0;
            }
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.POWER_CONTROL, "", newState);
            mainapp.buttonVibration();
        }
    }

    //Set the button text based on current power state  TODO: improve code 
    public void refresh_power_control_view() {
        Button b = findViewById(R.id.power_control_button);
        Drawable currentImage = power_unknown_drawable;
        if (!mainapp.isPowerControlAllowed()) {
            b.setEnabled(false);
            TextView tv = findViewById(R.id.power_control_text);
            tv.setText(getString(R.string.power_control_not_allowed));
        } else {
            b.setEnabled(true);
            switch (mainapp.power_state) {
                case "1":
                    currentImage = power_on_drawable;
                    break;
                case "2":
                    currentImage = power_unknown_drawable;
                    break;
                default:
                    currentImage = power_off_drawable;
                    break;
            }
        }

        if (PMenu != null) {
            mainapp.displayEStop(PMenu);
        }

        b.setBackgroundDrawable(currentImage);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        mainapp.applyTheme(this);

        setContentView(R.layout.power_control);

        //put pointer to this activity's handler in main app's shared variable (If needed)
        mainapp.power_control_msg_handler = new power_control_handler();

        power_on_drawable = getResources().getDrawable(R.drawable.power_green);
        power_off_drawable = getResources().getDrawable(R.drawable.power_red);
        power_unknown_drawable = getResources().getDrawable(R.drawable.power_yellow);


        //Set the button callbacks, storing the command to pass for each
        Button b = findViewById(R.id.power_control_button);
        button_listener click_listener = new button_listener();
        b.setOnClickListener(click_listener);

        Button closeButton = findViewById(R.id.power_button_close);
        close_button_listener close_click_listener = new close_button_listener();
        closeButton.setOnClickListener(close_click_listener);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_power_control),
                    "");
        }

    } // end onCreate

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) { //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        if (PMenu != null) {
            mainapp.displayEStop(PMenu);
            mainapp.displayFlashlightMenuButton(PMenu);
            mainapp.setFlashlightButton(PMenu);
        }
        //update power state
        refresh_power_control_view();
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mainapp.power_control_msg_handler !=null) {
            mainapp.power_control_msg_handler.removeCallbacksAndMessages(null);
            mainapp.power_control_msg_handler = null;
        } else {
            Log.d("Engine_Driver", "onDestroy: mainapp.power_control_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.power_menu, menu);
        PMenu = menu;
        mainapp.displayEStop(menu);
        mainapp.displayFlashlightMenuButton(PMenu);
        mainapp.setFlashlightButton(PMenu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                mainapp.buttonVibration();
                return true;
            case R.id.flashlight_button:
                mainapp.toggleFlashlight(this, PMenu);
                mainapp.buttonVibration();
                return true;
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    mainapp.powerControlNotAllowedDialog(PMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                mainapp.buttonVibration();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    private void disconnect() {
        this.finish();
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            finish();
        }
    }

    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        boolean rslt = mainapp.implDispatchGenericMotionEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    // listener for physical keyboard events
    // used to support the gamepad only   DPAD and key events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        InputDevice idev = getDevice(event.getDeviceId());
        boolean rslt = mainapp.implDispatchKeyEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }
}
