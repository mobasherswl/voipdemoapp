package com.example.voiptest;

import java.util.ArrayList;
import java.util.List;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.impl.NgnConfigurationService;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.tinyWRAP.SipStack;
import org.doubango.tinyWRAP.tdav_codec_id_t;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.Toast;

public class CodecSettingActivity extends Activity {
	
	private tdav_codec_id_t_wrapper[] sArray;
	private TouchInterceptor lstView;
	private int codecs;
	private static NgnConfigurationService configService = (NgnConfigurationService) NgnEngine
			.getInstance().getConfigurationService();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.codec_setting);
		
		lstView = (TouchInterceptor) findViewById(R.id.id_lst_codec);
		codecs = configService.getInt(NgnConfigurationEntry.MEDIA_CODECS, NgnConfigurationEntry.DEFAULT_MEDIA_CODECS);
		
		sArray = tdav_codec_id_t_wrapper.getCodecList();
		ArrayAdapter<tdav_codec_id_t_wrapper> adp = new ArrayAdapter<tdav_codec_id_t_wrapper>(this, android.R.layout.simple_list_item_checked, sArray) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView ctv = (CheckedTextView) super.getView(position, convertView, parent);
//				if(convertView == null) {
//					convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_checked, null, false);
//				}
//				CheckedTextView ctv = (CheckedTextView) convertView;
				ctv.setTag(sArray[position]);
				ctv.setChecked(sArray[position].isEnabled());
				ctv.setText(sArray[position].toString());

				return ctv;
			}
		};
		lstView.setAdapter(adp);
		lstView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long rowId) {
				CheckedTextView ctv = (CheckedTextView) view;
				ctv.setChecked(!ctv.isChecked());
				tdav_codec_id_t_wrapper codec = (tdav_codec_id_t_wrapper)ctv.getTag();
				codec.setEnabled(ctv.isChecked());
				int codecId = codec.getWrappedObject().swigValue();
				if((codecs & codecId) == codecId){
					codecs &= ~codecId;
				}
				else{
					codecs |= codecId;
				}

			}
		});
		lstView.setDropListener(mDropListener);

		registerForContextMenu(lstView);

	}
	
	public void onSave(View v) {
		if (sArray != null && sArray.length > 0) {
			tdav_codec_id_t_wrapper.saveCodecsToPref(sArray, codecs);
			applyCodecs();
			Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
		}
	}
	
	public static void applyCodecs() {
		SipStack.setCodecs_2(configService.getInt(NgnConfigurationEntry.MEDIA_CODECS, NgnConfigurationEntry.DEFAULT_MEDIA_CODECS));
		setCodecPriority(tdav_codec_id_t_wrapper.getCodecList());
	}

	private static void setCodecPriority(tdav_codec_id_t_wrapper[] srcArr) {
		int priority = 0;
		for(tdav_codec_id_t_wrapper obj : srcArr) {
			if(obj.isEnabled()) {
				SipStack.setCodecPriority(obj.getWrappedObject(), priority++);
			}
		}
	}

	private TouchInterceptor.DropListener mDropListener =

	new TouchInterceptor.DropListener() {

		public void drop(int from, int to) {

//			System.out.println("Droplisten from:" + from + " to:" + to);

			if (from == to) {
				return;
			}

			// Assuming that item is moved up the list
			int direction = -1;
			int loop_start = from;
			int loop_end = to;

			// For instance where the item is dragged down the list
			if (from < to) {
				direction = 1;
			}

			Object target = sArray[from];

			for (int i = loop_start; i != loop_end; i = i + direction) {
				sArray[i] = sArray[i + direction];
			}

			sArray[to] = (tdav_codec_id_t_wrapper) target;
//			System.out.println("Changed array is:" + Arrays.toString(sArray));
			((BaseAdapter) lstView.getAdapter()).notifyDataSetChanged();
		}
	};

}

class tdav_codec_id_t_wrapper {

	private tdav_codec_id_t m_tdav_codec_id_t;
	private String name;
	private boolean isEnabled;
	private static NgnConfigurationService configService;
	
	static
	{
		configService = (NgnConfigurationService) NgnEngine.getInstance().getConfigurationService();
	}
	
	public tdav_codec_id_t_wrapper(tdav_codec_id_t obj, boolean isEnabled) {
		m_tdav_codec_id_t = obj;
		name = m_tdav_codec_id_t.toString().substring(14).toUpperCase().replace('_', '-');
		this.setEnabled(isEnabled);
	}
	
	@Override
	public String toString() {
		// tdav_codec_id_amr_nb_oa
		return name;
	}
	
	public tdav_codec_id_t getWrappedObject() {
		return m_tdav_codec_id_t;
	}
	
	public boolean isEnabled() {
		return this.isEnabled;
	}
	
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isEnabled ? 1231 : 1237);
		result = prime
				* result
				+ ((m_tdav_codec_id_t == null) ? 0 : m_tdav_codec_id_t
						.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		tdav_codec_id_t_wrapper other = (tdav_codec_id_t_wrapper) obj;
		if (isEnabled != other.isEnabled)
			return false;
		if (m_tdav_codec_id_t != other.m_tdav_codec_id_t)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public static tdav_codec_id_t[] getWrappedObjectArray(tdav_codec_id_t_wrapper srcArr[]) {
		tdav_codec_id_t arr[] = new tdav_codec_id_t[srcArr.length];
		
		int i=0;
		for(tdav_codec_id_t_wrapper obj : srcArr) {
			arr[i++] = obj.getWrappedObject();
		}
		
		return arr;
	}
	
	public static tdav_codec_id_t_wrapper[] getWrapperObjectDefaultArray() {
		@SuppressWarnings("serial")
		ArrayList<tdav_codec_id_t> defaultSeq = new ArrayList<tdav_codec_id_t>() { 
			{
				add(tdav_codec_id_t.tdav_codec_id_g722);
				add(tdav_codec_id_t.tdav_codec_id_speex_wb);
				add(tdav_codec_id_t.tdav_codec_id_speex_uwb);
				add(tdav_codec_id_t.tdav_codec_id_speex_nb);
				add(tdav_codec_id_t.tdav_codec_id_pcma);
				add(tdav_codec_id_t.tdav_codec_id_pcmu);
				add(tdav_codec_id_t.tdav_codec_id_ilbc);
				add(tdav_codec_id_t.tdav_codec_id_gsm);
				add(tdav_codec_id_t.tdav_codec_id_g729ab);
				add(tdav_codec_id_t.tdav_codec_id_amr_nb_oa);
				add(tdav_codec_id_t.tdav_codec_id_amr_nb_be);
				add(tdav_codec_id_t.tdav_codec_id_h264_bp);
				add(tdav_codec_id_t.tdav_codec_id_h264_mp);
				add(tdav_codec_id_t.tdav_codec_id_vp8);
				add(tdav_codec_id_t.tdav_codec_id_mp4ves_es);
				add(tdav_codec_id_t.tdav_codec_id_theora);
				add(tdav_codec_id_t.tdav_codec_id_h263);
				add(tdav_codec_id_t.tdav_codec_id_h261);
			}
		};
		
		for(tdav_codec_id_t obj : tdav_codec_id_t.values()) {
			if(defaultSeq.contains(obj) == false) {
				defaultSeq.add(obj);
			}
		}
		
		return getWrapperObjectArray(defaultSeq.toArray(new tdav_codec_id_t[0]));
	}
	
	public static tdav_codec_id_t_wrapper[] getCodecList() {
		INgnConfigurationService pref = NgnEngine.getInstance().getConfigurationService();
		
		if(pref.getString("codecs", "").length() > 0) {
			return getWrapperObjectArrayFromPref();
		} else {
			return getWrapperObjectDefaultArray();
		}
	}
	
	public static tdav_codec_id_t_wrapper[] getWrapperObjectArray(tdav_codec_id_t srcArr[]) {
		return getWrapperObjectArray(srcArr, new boolean[srcArr.length - 1]);
	}

	public static tdav_codec_id_t_wrapper[] getWrapperObjectArray(tdav_codec_id_t srcArr[], boolean isEnabled[]) {
		List<tdav_codec_id_t_wrapper> arr = new ArrayList<tdav_codec_id_t_wrapper>(srcArr.length - 1);
		
		int codecs = configService.getInt(NgnConfigurationEntry.MEDIA_CODECS, NgnConfigurationEntry.DEFAULT_MEDIA_CODECS);
		for(tdav_codec_id_t obj : srcArr) {
			if(!obj.equals(tdav_codec_id_t.tdav_codec_id_none) && SipStack.isCodecSupported(obj)) {
				arr.add(new tdav_codec_id_t_wrapper(obj, ((obj.swigValue() & codecs) == obj.swigValue())));
			}
		}
		
		return arr.toArray(new tdav_codec_id_t_wrapper[0]);
	}
	
	public static tdav_codec_id_t_wrapper[] getWrapperObjectArrayFromPref() {
		String codecs = configService.getString("codecs", "");
		String codecArr[] = codecs.split(",");
		tdav_codec_id_t_wrapper wrapperArr[] = new tdav_codec_id_t_wrapper[codecArr.length];
		
		int i=0;
		for(String codec : codecArr) {
			wrapperArr[i] = new tdav_codec_id_t_wrapper(tdav_codec_id_t.valueOf(codec), configService.getBoolean(codec, false));
			i++;
		}
		
		return wrapperArr;
	}
	
	public static void saveCodecsToPref(tdav_codec_id_t_wrapper[] srcArr, int codecsStatus) {
		StringBuilder codecs = new StringBuilder(srcArr.length * 10);
		String wrappedObjName;

		for(tdav_codec_id_t_wrapper wrapper : srcArr) {
			wrappedObjName = wrapper.getWrappedObject().name();
			codecs.append(wrappedObjName).append(',');
			configService.putBoolean(wrappedObjName, wrapper.isEnabled());
		}

		codecs.delete(codecs.length() - 1, codecs.length());
		configService.putString("codecs", codecs.toString());
		configService.putInt(NgnConfigurationEntry.MEDIA_CODECS, codecsStatus);
		configService.commit();
	}
}
