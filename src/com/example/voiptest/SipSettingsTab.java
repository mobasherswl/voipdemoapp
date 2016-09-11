package com.example.voiptest;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class SipSettingsTab extends TabActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sip_settings_tab);
 
		TabHost tabHost = getTabHost(); 
 
		Intent intentSipSetting = new Intent().setClass(this, SipSetting.class);
		TabSpec tabSipSetting = tabHost
		  .newTabSpec("Config")
		  .setIndicator("Config")
		  .setContent(intentSipSetting);

		Intent intentCodec = new Intent().setClass(this, CodecSettingActivity.class);
		TabSpec tabCodec = tabHost
		  .newTabSpec("Codec")
		  .setIndicator("Codec")
		  .setContent(intentCodec);

		tabHost.addTab(tabSipSetting);
		tabHost.addTab(tabCodec);
 
		tabHost.setCurrentTab(0);
	}
}
