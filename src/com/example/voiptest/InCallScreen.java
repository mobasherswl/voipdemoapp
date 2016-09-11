package com.example.voiptest;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.events.NgnMediaPluginEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.model.NgnContact;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnInviteSession.InviteState;
import org.doubango.ngn.utils.NgnContentType;
import org.doubango.ngn.utils.NgnGraphicsUtils;
import org.doubango.ngn.utils.NgnStringUtils;
import org.doubango.ngn.utils.NgnUriUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class InCallScreen extends Activity {
	
	private static final String TAG = InCallScreen.class.getCanonicalName();
	
	private NgnEngine ngnEngine;
	private NgnAVSession mSession;
	private BroadcastReceiver broadcastReceiver;

	private TextView tvCallStatus;
	private TextView tvCallee;
	private TextView tvTimer;

	private Button btnAnswer;
	private Button btnHangUp;
	private Button btnMute;
	private Button btnSilent;

	private Handler callTimeHandler;
	private int callTimeMin;
	private int callTimeSec;

	private String remotePartyDisplayName;
	private Bitmap remotePartyPhoto;

	private boolean isVideoCall;
	private boolean inCall;
	private boolean isCallDone;
	private boolean isSilent;

	private OrientationEventListener orientListener;
	private int lastOrientation;
	private static int lastRotation;

	private Runnable callTimeHandlerRunnable = new Runnable() {
    	@Override
    	public void run() {
    		callTimeSec++;
    		if(callTimeSec > 59) {
    			callTimeSec = 0;
    			callTimeMin++;
    			if(callTimeMin > 59) {
    				callTimeMin = 0;
    			}
    		}
    		setCallDuration();
    		callTimeHandler.postDelayed(this, 1000);
    	}
    	
    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		Window window = this.getWindow();
	    window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
	    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
	    window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		setContentView(R.layout.in_call_screen);

		initWidgets();
	    
		Bundle extras = getIntent().getExtras();
        if(extras != null){
        	mSession = NgnAVSession.getSession(extras.getLong(CallService.SIP_SESSION_ID));
            if(mSession == null){
            	Log.e(TAG, "Null session");
            	if(extras.getLong(CallService.SIP_SESSION_ID) == 0) {
            		Toast.makeText(this, R.string.error_invalid_call_session, Toast.LENGTH_SHORT).show();
            	} else {
            		delayedFinish();
            	}
            	return;
            }
        }

        if(savedInstanceState != null) {
        	processRestart(savedInstanceState);
	    }

	    if(mSession.getState().equals(InviteState.INCOMING)) {
        	onIncoming();
        }

        mSession.incRef();
        mSession.setContext(this);
		
        initBroadcastReceiver();
        
	    ngnEngine = NgnEngine.getInstance();

	    updateRemotePartyInfo();
	    
	    isVideoCall = mSession.getMediaType() == NgnMediaType.AudioVideo || mSession.getMediaType() == NgnMediaType.Video;
	    
	    if(isVideoCall) {
	    	orientListener = getOrientationEventListener();
			if(!orientListener.canDetectOrientation()){
				Log.w(TAG, "canDetectOrientation() is equal to false");
			}
	    }
	    
	    prepareView();
	    
	    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
	}
	
	private void initWidgets() {
	    tvCallStatus = (TextView) findViewById(R.id.id_call_status);
	    tvCallee = (TextView) findViewById(R.id.id_callee);
	    tvTimer = (TextView) findViewById(R.id.id_tv_timer);
	    callTimeHandler = new Handler();
	    btnAnswer = (Button) findViewById(R.id.id_btn_answer);
	    btnHangUp = (Button) findViewById(R.id.id_btn_hang_up);
	    btnMute = (Button) findViewById(R.id.id_btn_mute);
	    btnSilent = (Button) findViewById(R.id.id_btn_silent);
	}

	private void processRestart(Bundle savedInstanceState) {
    	tvCallStatus.setText(savedInstanceState.getString("tvCallStatus"));
    	tvCallee.setText(savedInstanceState.getString("tvCallee"));
    	tvTimer.setText(savedInstanceState.getString("tvTimer"));
    	isSilent = savedInstanceState.getBoolean("isSilent");

    	if(mSession.getState().equals(InviteState.INCALL)) {
	        int timeDuration = (int) ((System.currentTimeMillis() - mSession.getStartTime()) / 1000);
	        callTimeMin = timeDuration / 60;
	        callTimeSec = timeDuration - (callTimeMin * 60);
        	inCall();
        	prepareView();
        } else {
        	setCallDuration();
        }
	}

	private void updateRemotePartyInfo() {
		final NgnContact remoteParty = ngnEngine.getContactService().getContactByUri(mSession.getRemotePartyUri());
		if(remoteParty != null){
			remotePartyDisplayName = remoteParty.getDisplayName();
			if((remotePartyPhoto = remoteParty.getPhoto()) != null){
				remotePartyPhoto = NgnGraphicsUtils.getResizedBitmap(remotePartyPhoto, 
						NgnGraphicsUtils.getSizeInPixel(128), NgnGraphicsUtils.getSizeInPixel(128));
			}
		}
		else{
			remotePartyDisplayName = NgnUriUtils.getDisplayName(mSession.getRemotePartyUri());
		}
		if(NgnStringUtils.isNullOrEmpty(remotePartyDisplayName)){
			remotePartyDisplayName = "Unknown";
		}
		
	    tvCallee.setText(remotePartyDisplayName);
	}

	private void applyCamRotation(int rotation){
		if(mSession != null){
			lastRotation = rotation;
			// libYUV
			mSession.setRotation(rotation);
		}
	}

	private void prepareView() {

		updateMute(mSession.isMicrophoneMute());
		
		isVideoCall = mSession.getMediaType() == NgnMediaType.AudioVideo || mSession.getMediaType() == NgnMediaType.Video;
		
		ImageView imgView = (ImageView) findViewById(R.id.id_image_remote);
		
		if(remotePartyPhoto != null) {
			imgView.setImageBitmap(remotePartyPhoto);
		}
		if(isVideoCall) {
			imgView.setVisibility(View.GONE);
			View v = mSession.startVideoConsumerPreview();
			FrameLayout fl = (FrameLayout) findViewById(R.id.id_layout_remote_video);
			if(v != null) {
				if(v.getParent() != null) {
					((ViewGroup)v.getParent()).removeView(v);
				}
				fl.setVisibility(View.VISIBLE);
				fl.addView(v);
			}
			
			mSession.setSendingVideo(true);
			v = mSession.startVideoProducerPreview();
			fl = (FrameLayout) findViewById(R.id.id_layout_local_video);
			if(v != null) {
				if(v.getParent() != null) {
					((ViewGroup)v.getParent()).removeView(v);
				}
				if(v instanceof SurfaceView){
					((SurfaceView)v).setZOrderOnTop(true);
				}
				fl.addView(v);
				fl.bringChildToFront(v);
			}
		} else {
			
		}
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState) {
		outState.putString("tvTimer", tvTimer.getText().toString());
		outState.putString("tvCallee", tvCallee.getText().toString());
		outState.putString("tvCallStatus", tvCallStatus.getText().toString());
		outState.putBoolean("isSilent", isSilent);
	}
	
	public void onHangUp(View v) {
		if(isCallDone == false) {
			isCallDone = true;
			if(mSession != null) {
				mSession.hangUpCall();
			}
		}
	}
	
	public void onMute(View v) {
		if(mSession != null) {
			updateMute(!mSession.isMicrophoneMute());
		}
	}
	
	private void updateMute(boolean isMute) {
		mSession.setMicrophoneMute(isMute);
		if(isMute) {
			btnMute.setBackgroundResource(R.drawable.mute);
		} else {
			btnMute.setBackgroundResource(R.drawable.unmute);
		}
	}
	
	private String getStateDesc(InviteState state){
		switch(state){
			case NONE:
			default:
				return "Unknown";
			case INCOMING:
				return "Incoming";
			case INPROGRESS:
				return "In Progress";
			case REMOTE_RINGING:
				return "Ringing";
			case EARLY_MEDIA:
				return "Early media";
			case INCALL:
				return "In Call";
			case TERMINATING:
//				return "Terminating";
			case TERMINATED:
				return "Terminated";
		}
	}

	private void handleSipEvent(Intent intent){
		if(mSession == null){
			Log.e(TAG, "Invalid session object");
			return;
		}
		final String action = intent.getAction();
		if(NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(action)){
			NgnInviteEventArgs args = intent.getParcelableExtra(NgnInviteEventArgs.EXTRA_EMBEDDED);
			if(args == null){
				Log.e(TAG, "Invalid event args");
				return;
			}
			if(args.getSessionId() != mSession.getId()){
				return;
			}
			processCallStates(mSession.getState());
		}
	}
	
	private void processCallStates(InviteState callState) {

		tvCallStatus.setText(getStateDesc(callState));

		switch(callState){
		case REMOTE_RINGING:
			break;
		case INCOMING:
			inComing();
			break;
		case EARLY_MEDIA:
			break;
		case INCALL:
			inCall();
			break;
		case TERMINATING:
		case TERMINATED:
		case NONE:
			delayedFinish();
			break;
		default:
				break;
		}

	}

	private void inCall() {
		if(inCall == false) {
			inCall = true;
			startCallTimer();
			btnSilent.setVisibility(View.GONE);
			btnMute.setVisibility(View.VISIBLE);
		}
		mSession.setSpeakerphoneOn(false);
	}
	
	private void inComing() {
		if(btnSilent.getVisibility() == View.GONE) {
			btnSilent.setVisibility(View.VISIBLE);
			updateOnSilent();
		}
	}
	
	private void startCallTimer() {
		callTimeHandler.postDelayed(callTimeHandlerRunnable, 1000);		
	}
	
	private void delayedFinish() {
		btnHangUp.setBackgroundResource(R.drawable.phone_hang_up_clicked_48);
		btnMute.setVisibility(View.GONE);
		btnAnswer.setVisibility(View.GONE);
		btnSilent.setVisibility(View.GONE);
		if(mSession != null) {
			isCallDone = true;
			callTimeHandler.removeCallbacks(callTimeHandlerRunnable);
			callTimeHandler.postDelayed(new Runnable() {
	
				@Override
				public void run() {
					finish();
				}
				
			}, 2000);
		} else {
			finish();
		}
	}
	
	public void onSilent(View v) {
		if(!isSilent) {
			isSilent = true;
			ngnEngine.getSoundService().stopRingTone();
			updateOnSilent();
		}
	}
	
	private void updateOnSilent() {
		if(mSession != null && isSilent) {
			btnSilent.setBackgroundResource(R.drawable.phone_ringing_cancel_greyed);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG,"onResume()");
//		if(mSession != null){
			processCallStates(mSession.getState());
//			final InviteState callState = mSession.getState();
//			tvCallStatus.setText(getStateDesc(callState));
//			if(callState == InviteState.TERMINATING || callState == InviteState.TERMINATED){
//				delayedFinish();
//			}
//		} 
	}

	@Override
	public void onBackPressed() {
		
	}
	
	@Override
	protected void onDestroy() {
		Log.d(TAG,"onDestroy()");
       if(broadcastReceiver != null){
    	   unregisterReceiver(broadcastReceiver);
    	   broadcastReceiver = null;
       }
       
       if(mSession != null && isCallDone){
    	   mSession.setContext(null);
    	   mSession.decRef();
    	   if(!mSession.isActive()) {
    		   NgnAVSession.releaseSession(mSession);
    	   }
       }
       super.onDestroy();
	}

	private void handleMediaEvent(Intent intent){
		final String action = intent.getAction();
	
		if(NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT.equals(action)){
			NgnMediaPluginEventArgs args = intent.getParcelableExtra(NgnMediaPluginEventArgs.EXTRA_EMBEDDED);
			if(args == null){
				Log.e(TAG, "Invalid event args");
				return;
			}
			
			switch(args.getEventType()){
				case STARTED_OK: //started or restarted (e.g. reINVITE)
				{
					isVideoCall = (mSession.getMediaType() == NgnMediaType.AudioVideo || mSession.getMediaType() == NgnMediaType.Video);
					prepareView();
//					if(isVideoCall) {
//						startActivity(getIntent());
//						finish();
//					}
					
					break;
				}
				case PREPARED_OK:
				case PREPARED_NOK:
				case STARTED_NOK:
				case STOPPED_OK:
				case STOPPED_NOK:
				case PAUSED_OK:
				case PAUSED_NOK:
				{
					break;
				}
			}
		}
	}
	
	private OrientationEventListener getOrientationEventListener() {
		return new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
			@Override
			public void onOrientationChanged(int orient) {
				try {				
					if ((orient > 345 || orient <15)  ||
							(orient > 75 && orient <105)   ||
							(orient > 165 && orient < 195) ||
							(orient > 255 && orient < 285)){
						int rotation = mSession.compensCamRotation(true);
						if (rotation != lastRotation) {
							applyCamRotation(rotation);
							if(mSession != null){
								final android.content.res.Configuration conf = getResources().getConfiguration();
								if(conf.orientation != lastOrientation){
									lastOrientation = conf.orientation;
									switch(lastOrientation){
										case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
											mSession.sendInfo("orientation:landscape\r\nlang:en-EN\r\n", NgnContentType.DOUBANGO_DEVICE_INFO);
											break;
										case android.content.res.Configuration.ORIENTATION_PORTRAIT:
											mSession.sendInfo("orientation:portrait\r\nlang:en-EN\r\n", NgnContentType.DOUBANGO_DEVICE_INFO);
											break;
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}

	private void initBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(intent.getAction())){
					handleSipEvent(intent);
				} else if(NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT.equals(intent.getAction())){
					handleMediaEvent(intent);
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NgnInviteEventArgs.ACTION_INVITE_EVENT);
		intentFilter.addAction(NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT);
	    registerReceiver(broadcastReceiver, intentFilter);
	}
	
	private void setCallDuration() {
		tvTimer.setText(getTimeStringValue(callTimeMin) + ":" + getTimeStringValue(callTimeSec));
	}

	private String getTimeStringValue(int time) {
		return time < 10 ? String.format("0%d", time) : String.valueOf(time);
	}
	
	private void onIncoming() {
		btnAnswer.setVisibility(View.VISIBLE);
	}
	
	public void onAnswer(View v) {
		if(mSession != null && !mSession.getState().equals(InviteState.INCALL)) {
			mSession.acceptCall();

			btnAnswer.setVisibility(View.GONE);
		}
	}
}
