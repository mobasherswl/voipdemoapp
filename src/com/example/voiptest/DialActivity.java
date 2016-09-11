package com.example.voiptest;

import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voiptest.CallService.LocalBinder;

public class DialActivity extends Activity {
	
	private TextView phoneNum;
	private CallService callService;
    private boolean mBound = false;
    private BroadcastReceiver broadcastReciever;
    private Intent intentCallService;
    private int retryCount = 0;
    private TextView regStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dial);
        
        regStatus = (TextView) findViewById(R.id.id_reg_status);
        phoneNum = (TextView) findViewById(R.id.id_phone_num);
        
        if(savedInstanceState != null && savedInstanceState.getString(CallService.PHONE_NUMBER) != null) {
        	phoneNum.setText(savedInstanceState.getString(CallService.PHONE_NUMBER));
        }
        
        initButtons();
        
        intentCallService = new Intent(this, CallService.class);
        startService(intentCallService);
        bindService(intentCallService, mConnection, Context.BIND_AUTO_CREATE);
        
        broadcastReciever = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();

				// Registration Event
				if(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT.equals(action)){
					NgnRegistrationEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);
					if(args == null){
						regStatus.setText("Invalid event args");
						return;
					}
					switch(args.getEventType()){
						case REGISTRATION_NOK:
							regStatus.setText("Failed to register");
							break;
						case UNREGISTRATION_OK:
							regStatus.setText("Unregistered");
							break;
						case REGISTRATION_OK:
							regStatus.setText("Registered");
							break;
						case REGISTRATION_INPROGRESS:
							regStatus.setText("Trying to register...");
							break;
						case UNREGISTRATION_INPROGRESS:
							regStatus.setText("Trying to unregister...");
							break;
						case UNREGISTRATION_NOK:
							regStatus.setText("Failed to unregister");
							break;
					}
				}
			}
		};

		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT);
	    registerReceiver(broadcastReciever, intentFilter);

    }


    private void initButtons() {

    	initButton(findViewById(R.id.id_btn_num_0), "0", "ABC", KeyEvent.KEYCODE_0, true);    	
    	initButton(findViewById(R.id.id_btn_num_1), "1", "ABC", KeyEvent.KEYCODE_1, true);
    	initButton(findViewById(R.id.id_btn_num_2), "2", "ABC", KeyEvent.KEYCODE_2, false);
    	initButton(findViewById(R.id.id_btn_num_3), "3", "DEF", KeyEvent.KEYCODE_3, false);
    	initButton(findViewById(R.id.id_btn_num_4), "4", "GHI", KeyEvent.KEYCODE_4, false);
    	initButton(findViewById(R.id.id_btn_num_5), "5", "JKL", KeyEvent.KEYCODE_5, false);
    	initButton(findViewById(R.id.id_btn_num_6), "6", "MNO", KeyEvent.KEYCODE_6, false);
    	initButton(findViewById(R.id.id_btn_num_7), "7", "PQRS", KeyEvent.KEYCODE_7, false);
    	initButton(findViewById(R.id.id_btn_num_8), "8", "TUV", KeyEvent.KEYCODE_8, false);
    	initButton(findViewById(R.id.id_btn_num_9), "9", "WXYZ", KeyEvent.KEYCODE_9, false);
    	initButton(findViewById(R.id.id_btn_plus), "+", "ABC", KeyEvent.KEYCODE_PLUS, true);
    	initButton(findViewById(R.id.id_btn_bksp), "Del", "", KeyEvent.KEYCODE_DEL, false);
    	initButton(findViewById(R.id.id_btn_voice_call), "Voice Call", "", 1000, false);
    	initButton(findViewById(R.id.id_btn_video_call), "Video Call", "", 1001, false);
		
	}


	private void initButton(View view, String num, String text,
			int tag, boolean hideText) {
    	
		view.setTag(tag);
		((TextView)view.findViewById(R.id.id_tv_num)).setText(num);
		TextView tvText = (TextView) view.findViewById(R.id.id_tv_text);
		tvText.setText(text);
		if(hideText) {
			tvText.setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
		return updateInput(keyCode, keyEvent);
	}

	@Override
	protected void onSaveInstanceState (Bundle outState) {
		outState.putString(CallService.PHONE_NUMBER, phoneNum.getText().toString());
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SipSettingsTab.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	checkServiceStatus();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	retryCount = 0;
    }

    public void onBtnClick(View v) {
		updateInput(((Integer)v.getTag()), null);

    }
    
    private boolean updateInput(int key, KeyEvent keyEvent) {
    	boolean isHandled = true;
		char ch = 0;
		String num = phoneNum.getText().toString();
		
		if(keyEvent != null && Character.isDigit(keyEvent.getNumber())) {
			ch = keyEvent.getNumber();
		}

		switch(key) {
    	case KeyEvent.KEYCODE_PLUS: //+
    		ch = '+';
    		break;
    	case KeyEvent.KEYCODE_DEL: //Bksp
        	if(num.length() > 0) {
        		phoneNum.setText(num.substring(0, num.length() - 1));
        	}  
    		break;
    	case 1000: // voice call
    		makeCall(num, NgnMediaType.Audio);
    		break;
    	case 1001: // video call
    		makeCall(num, NgnMediaType.AudioVideo);
    		break;
    	case KeyEvent.KEYCODE_0:
			ch = '0';
			break;
   		case KeyEvent.KEYCODE_1:
			ch = '1';
			break;
   		case KeyEvent.KEYCODE_2:
			ch = '2';
			break;
    	case KeyEvent.KEYCODE_3:
			ch = '3';
			break;
    	case KeyEvent.KEYCODE_4:
			ch = '4';
			break;
    	case KeyEvent.KEYCODE_5:
			ch = '5';
			break;
    	case KeyEvent.KEYCODE_6:
			ch = '6';
			break;
    	case KeyEvent.KEYCODE_7:
			ch = '7';
			break;
    	case KeyEvent.KEYCODE_8:
			ch = '8';
			break;
    	case KeyEvent.KEYCODE_9:
			ch = '9';
			break;
    	case KeyEvent.KEYCODE_BACK:
    		finish();
    		break;
    	default:
    		isHandled = false;
    	}
    	if(phoneNum.getText().length() < 21 && ch != 0) {
    		phoneNum.setText(phoneNum.getText().toString() + ch);
    	}
    	
    	return isHandled;
    }
    
    private void makeCall(String num, NgnMediaType mediaType) {
		if(callService!= null && mBound && callService.isRegistered()) {
			if(num.length() > 0) {
	
	        	if(callService.makeCall(phoneNum.getText().toString(), InCallScreen.class, mediaType) == false) {
	    			Toast.makeText(
	    					this,
	    					"Call initiation failed. Check your SIP & settings and make sure the number is correct as well. ",
	    					Toast.LENGTH_LONG).show();
	        	}
			} else {
    			Toast.makeText(
    					this,
    					R.string.error_no_num_to_dial,
    					Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(
					this,
					R.string.error_unable_to_dial,
					Toast.LENGTH_LONG).show();
		}
    }

    private void checkServiceStatus() {
      	if(callService!= null) {
      		if(callService.isRegistered() == false) {
	      		if(retryCount < 1) {
		      		retryCount++;
		      		if(callService.reRegister() == false) {
		      			unbindService(mConnection);
		      			if(stopService(intentCallService)) {
		      				startService(intentCallService);
		      				bindService(intentCallService, mConnection, Context.BIND_AUTO_CREATE);
		      			}
		      		}
	      		}
      		} else {
      			regStatus.setText("Registered");
      		}
      	}
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            callService = binder.getService();
            mBound = true;
          	checkServiceStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if(broadcastReciever != null) {
    		unregisterReceiver(broadcastReciever);
    	}
  		unbindService(mConnection);
    }
}
