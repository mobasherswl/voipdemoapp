package com.example.voiptest;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.NgnNativeService;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnUriUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

public class CallService extends NgnNativeService {
	
	public static final String SIP_SESSION_ID = "sip_session_id";
	public static final String PHONE_NUMBER = "phone_number";
	public static final String IS_DEFAULT_CONF_SET = "is_default_conf_set";
	private final IBinder mBinder = new LocalBinder();
	private NgnEngine ngnEngine;
	private INgnSipService ngnSipService;
	private INgnConfigurationService mConfigurationService;
	private BroadcastReceiver mBroadcastReceiver;
	
	public CallService() {
		ngnEngine = NgnEngine.getInstance();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		if(!ngnEngine.isStarted()){
			ngnEngine.start();
		}
		CodecSettingActivity.applyCodecs();
		mConfigurationService = ngnEngine.getConfigurationService();
		
		if(mConfigurationService.getBoolean(IS_DEFAULT_CONF_SET, false) == false) {
//			setDefaultConfig();
		}
		// Register
		if(ngnEngine.isStarted()){
			ngnSipService = ngnEngine.getSipService();
			register();
		}

		mBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();

				// Invite Events
				if(NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(action)){
					NgnInviteEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);
					if(args == null){
						return;
					}
					
					final NgnMediaType mediaType = args.getMediaType();
					
					switch(args.getEventType()){							
						case TERMWAIT:
						case TERMINATED:
							if(NgnMediaType.isAudioVideoType(mediaType)){
								ngnEngine.getSoundService().stopRingBackTone();
								ngnEngine.getSoundService().stopRingTone();
							}
							break;
							
						case INCOMING:
							if(NgnMediaType.isAudioVideoType(mediaType)){
								final NgnAVSession avSession = NgnAVSession.getSession(args.getSessionId());
								if(avSession != null){
									ngnEngine.getSoundService().startRingTone();
									startInCallActivity(avSession.getRemotePartyDisplayName(), InCallScreen.class, avSession);
								}
							}
							break;
							
						case INPROGRESS:
							break;
						case RINGING:
							if(NgnMediaType.isAudioVideoType(mediaType)){
								ngnEngine.getSoundService().startRingBackTone();
							}
							break;
						
						case CONNECTED:
						case EARLY_MEDIA:
							if(NgnMediaType.isAudioVideoType(mediaType)){
								ngnEngine.getSoundService().stopRingBackTone();
								ngnEngine.getSoundService().stopRingTone();
							}
							break;
						default: break;
					}
				}

			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NgnInviteEventArgs.ACTION_INVITE_EVENT);
	    registerReceiver(mBroadcastReceiver, intentFilter);

	}
	
	private void setDefaultConfig() {
		mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI, /*SIP_USERNAME*/"453226363249");
		mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU, String.format("sip:%s@%s", /*SIP_USERNAME*/"453226363249", /*SIP_DOMAIN*/"rtsip.vopium.com"));
		mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD, /*SIP_PASSWORD*/"password");
		mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST, /*SIP_SERVER_HOST*/"rtsip.vopium.com");
		mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT, /*SIP_SERVER_PORT*/5060);
		mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM, /*SIP_DOMAIN*/"rtsip.vopium.com");
		mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G, true);
		mConfigurationService.putBoolean(IS_DEFAULT_CONF_SET, true);
		mConfigurationService.putBoolean(NgnConfigurationEntry.GENERAL_FULL_SCREEN_VIDEO, false);
		// VERY IMPORTANT: Commit changes
		mConfigurationService.commit();
	}

	public boolean register() {

		if(!ngnSipService.isRegistered()){
			return ngnSipService.register(this);
		}
		return ngnSipService.isRegistered();
	}
	
	public boolean isRegistered() {
		return ngnSipService.isRegistered();
	}
	
	public boolean reRegister() {
		return ngnSipService.unRegister() && register(); 
	}
	
	boolean makeVoiceCall(String phoneNumber, Class<?> classObj){
		return makeCall(phoneNumber, classObj, NgnMediaType.Audio);
	}

	boolean makeVideoCall(String phoneNumber, Class<?> classObj){
		return makeCall(phoneNumber, classObj, NgnMediaType.AudioVideo);
	}
	
	boolean makeCall(String phoneNumber, Class<?> classObj, NgnMediaType mediaType){
		final String validUri = NgnUriUtils.makeValidSipUri(String.format("sip:%s@%s", phoneNumber, mConfigurationService.getString(NgnConfigurationEntry.NETWORK_REALM, "")));
		if(validUri == null){
//			Toast.makeText(NgnApplication.getInstance(), "failed to normalize sip uri '" + phoneNumber + "'", Toast.LENGTH_LONG).show();
			return false;
		}
		NgnAVSession avSession = NgnAVSession.createOutgoingSession(ngnSipService.getSipStack(), mediaType);
		
		startInCallActivity(phoneNumber, classObj, avSession);
		
		return avSession.makeCall(validUri);
	}

	public void startInCallActivity(String phoneNumber, Class<?> classObj, NgnAVSession avSession) {
		Intent intent = new Intent();
		intent.setClass(this, classObj);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(SIP_SESSION_ID, avSession.getId());
		intent.putExtra(PHONE_NUMBER, phoneNumber);
		startActivity(intent);
	}
	
	public class LocalBinder extends Binder {

		CallService getService() {

            return CallService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public void onDestroy() {
    	ngnSipService.unRegister();
    	ngnEngine.stop();
    	if(mBroadcastReceiver != null) {
    		unregisterReceiver(mBroadcastReceiver);
    	}
    }
}
