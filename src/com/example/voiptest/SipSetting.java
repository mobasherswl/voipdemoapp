package com.example.voiptest;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.utils.NgnConfigurationEntry;

import com.example.voiptest.CallService.LocalBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SipSetting extends Activity {

	private CallService callService;
    private boolean mBound = false;
    private Intent intentCallService;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etDomain;
    private EditText etProxy;
    private EditText etPort;
    private CheckBox cbIce;
    private CheckBox cbStun;
    private EditText etStunServer;
    private EditText etStunPort;
    private LinearLayout linearLayoutStun;
    private INgnConfigurationService mConfigurationService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.sip_settings);

		intentCallService = new Intent(this, CallService.class);
		getApplicationContext().bindService(intentCallService, mConnection, Context.BIND_AUTO_CREATE);

		mConfigurationService = NgnEngine.getInstance().getConfigurationService();

		etUsername = (EditText) findViewById(R.id.id_et_username);
		etPassword = (EditText) findViewById(R.id.id_et_password);
		etDomain = (EditText) findViewById(R.id.id_et_domain);
		etProxy = (EditText) findViewById(R.id.id_et_proxy);
		etPort = (EditText) findViewById(R.id.id_et_port);
		cbIce = (CheckBox) findViewById(R.id.id_cb_ice);
		cbStun = (CheckBox) findViewById(R.id.id_cb_stun_turn);
		etStunServer = (EditText) findViewById(R.id.id_et_stun);
		etStunPort = (EditText) findViewById(R.id.id_et_stun_port);
		linearLayoutStun = (LinearLayout) findViewById(R.id.id_layout_stun_turn);
		
		populateFields(savedInstanceState);

	}
	
	private void populateFields(Bundle savedInstanceState) {
		if(savedInstanceState == null) {
			etUsername.setText(mConfigurationService.getString(NgnConfigurationEntry.IDENTITY_IMPI, ""));
			etPassword.setText(mConfigurationService.getString(NgnConfigurationEntry.IDENTITY_PASSWORD, ""));
			etDomain.setText(mConfigurationService.getString(NgnConfigurationEntry.NETWORK_REALM, ""));
			etProxy.setText(mConfigurationService.getString(NgnConfigurationEntry.NETWORK_PCSCF_HOST, ""));
			int port = mConfigurationService.getInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT, -1);
			etPort.setText(port == -1 ? getString(R.string.default_sip_port) : String.valueOf(port));
			cbIce.setChecked(mConfigurationService.getBoolean(NgnConfigurationEntry.NATT_USE_ICE, false));
			cbStun.setChecked(mConfigurationService.getBoolean(NgnConfigurationEntry.NATT_USE_STUN, false));
			toggleStunSettings();
			etStunServer.setText(mConfigurationService.getString(NgnConfigurationEntry.NATT_STUN_SERVER, ""));
			port = mConfigurationService.getInt(NgnConfigurationEntry.NATT_STUN_PORT, -1);
			etStunPort.setText(port == -1 ? getString(R.string.default_stun_port) : String.valueOf(port));
		}
	}

	@Override
	protected void onSaveInstanceState (Bundle savedInstanceState) {
		savedInstanceState.putBoolean("is_stun_enabled", cbStun.isChecked());
	}

	@Override
	protected void onRestoreInstanceState (Bundle savedInstanceState) {
		if(savedInstanceState != null && savedInstanceState.getBoolean("is_stun_enabled")) {
			cbStun.setChecked(true);
			toggleStunSettings();
		}
	}
	
	private void toggleStunSettings() {
		if(cbStun.isChecked()) {
			linearLayoutStun.setVisibility(View.VISIBLE);
		} else {
			linearLayoutStun.setVisibility(View.GONE);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
//		getApplicationContext().unbindService(mConnection);
	}
	
	public void onSave(View v) {
		v.setEnabled(false);
		
		String username = etUsername.getText().toString();
		String password = etPassword.getText().toString();
		String domain = etDomain.getText().toString();
		String proxy = etProxy.getText().toString();
		int port = Integer.parseInt(etPort.getText().toString());
		boolean isIceEnabled = cbIce.isChecked();
		boolean isStunEnabled = cbStun.isChecked();
		String stunServer = etStunServer.getText().toString();
		int stunPort = Integer.parseInt(etStunPort.getText().toString()); 
		
		if(!username.equalsIgnoreCase(mConfigurationService.getString(NgnConfigurationEntry.IDENTITY_IMPI, "")) 
				|| !password.equals(mConfigurationService.getString(NgnConfigurationEntry.IDENTITY_PASSWORD, ""))
				|| !domain.equalsIgnoreCase(mConfigurationService.getString(NgnConfigurationEntry.NETWORK_REALM, ""))
				|| !proxy.equalsIgnoreCase(mConfigurationService.getString(NgnConfigurationEntry.NETWORK_PCSCF_HOST, ""))
				|| port != mConfigurationService.getInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT, 0)
				|| isIceEnabled != mConfigurationService.getBoolean(NgnConfigurationEntry.NATT_USE_ICE, false)
				|| isStunEnabled != mConfigurationService.getBoolean(NgnConfigurationEntry.NATT_USE_STUN, false)
				|| !stunServer.equalsIgnoreCase(mConfigurationService.getString(NgnConfigurationEntry.NATT_STUN_SERVER, ""))
				|| stunPort != mConfigurationService.getInt(NgnConfigurationEntry.NATT_STUN_PORT, 0)) {

			mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI, /*SIP_USERNAME*/username);
			mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU, String.format("sip:%s@%s", /*SIP_USERNAME*/username, /*SIP_DOMAIN*/domain));
			mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD, /*SIP_PASSWORD*/password);
			mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST, /*SIP_SERVER_HOST*/proxy);
			mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT, /*SIP_SERVER_PORT*/port);
			mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM, /*SIP_DOMAIN*/domain);
			mConfigurationService.putBoolean(NgnConfigurationEntry.NATT_USE_ICE, isIceEnabled);
			mConfigurationService.putBoolean(NgnConfigurationEntry.NATT_USE_STUN, isStunEnabled);
			if(isStunEnabled && (
					!stunServer.equalsIgnoreCase(mConfigurationService.getString(NgnConfigurationEntry.NATT_STUN_SERVER, ""))
					|| stunPort != mConfigurationService.getInt(NgnConfigurationEntry.NATT_STUN_PORT, 0))) {
				mConfigurationService.putString(NgnConfigurationEntry.NATT_STUN_SERVER, stunServer);
				mConfigurationService.putInt(NgnConfigurationEntry.NATT_STUN_PORT, stunPort);
			}
			mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G, true);
			// VERY IMPORTANT: Commit changes
			mConfigurationService.commit();
			
			if(callService != null && mBound) {
				if(callService.reRegister()) {
					Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, R.string.reg_failed, Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(this, R.string.settings_not_applied, Toast.LENGTH_SHORT).show();
			}

		} else {
			Toast.makeText(this, R.string.settings_already_saved, Toast.LENGTH_SHORT).show();
		}
		
		v.setEnabled(true);
	}

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            callService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    public void onCbStunTurn(View v) {
    	toggleStunSettings();
    }

}
