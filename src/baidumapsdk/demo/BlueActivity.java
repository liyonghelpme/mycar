package baidumapsdk.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.liyong.map.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class BlueActivity extends Activity {
	final Context con = this;
	Button scan;
	Set<BluetoothDevice> pairedDevices;
	BluetoothAdapter mBluetoothAdapter;
	List<String> mdevice;
	ListView lv;
	StableArrayAdapter adapter;
	private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	BluetoothSocket bsocket;
	OutputStream outstream;
	
	public int ena = 1;
	private final BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context arg0, Intent intent) {
			// TODO Auto-generated method stub
			
			String action = intent.getAction();
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.i("BLUE", "find device "+device.getName());
				//mdevice.add(device.getName()+"\n"+device.getAddress());
				adapter.add(device.getName()+"\n"+device.getAddress());
			}
		}
		
	};
	@Override
	public void onCreate(Bundle saved){
		super.onCreate(saved);
		setContentView(R.layout.blue);
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		mdevice = new ArrayList<String>();
		lv = (ListView)findViewById(R.id.listView1);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null){
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setMessage("设备不支持蓝牙!!")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					BlueActivity.this.finish();
				}
			});
			AlertDialog a = b.create();
			a.show();
		} else {
			initBluetooth();
		}
	}
	@Override
	public void onDestroy(){
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}
	public void initBluetooth(){
		if(!mBluetoothAdapter.isEnabled()){
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, ena);
		} else {
			pairedDevices = mBluetoothAdapter.getBondedDevices();
			if(pairedDevices.size() > 0){
				for(BluetoothDevice device : pairedDevices){
					mdevice.add(device.getName()+"\n"+device.getAddress());
				}
			}
			adapter = new StableArrayAdapter(con, android.R.layout.simple_list_item_1, mdevice);
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					final String item = (String)parent.getItemAtPosition(position);
					Log.i("BLUE", "touch item "+item);
					String[] val = item.split("\n");
					Log.i("BLUE", val[1]);
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(val[1]);
					try {
						bsocket = createBluetoothSocket(device);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						BlueActivity.this.finish();
					}
					mBluetoothAdapter.cancelDiscovery();
					Log.d("BLUE", "connecting..."+val[0]+" "+val[1]);
					try {
						bsocket.connect();
						Log.d("BLUE", "connectin ok");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Log.d("BLUE", "...create socket");
					try {
						outstream = bsocket.getOutputStream();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			});
			
			
			
			scan = (Button)findViewById(R.id.button1);
			scan.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View arg0) {
					boolean sd = mBluetoothAdapter.startDiscovery();
					Log.i("BLUE", "start scan");
				}
			});
		}
	}
	 private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
	      if(Build.VERSION.SDK_INT >= 10){
	          try {
	              final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
	              return (BluetoothSocket) m.invoke(device, myUUID);
	          } catch (Exception e) {
	              Log.e("BLUE", "Could not create Insecure RFComm Connection",e);
	          }
	      }
	      return  device.createRfcommSocketToServiceRecord(myUUID);
	  }
	 
	private class StableArrayAdapter extends ArrayAdapter<String>{
		HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();
		public StableArrayAdapter(Context context,
				int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			// TODO Auto-generated constructor stub
			for(int i=0; i<objects.size(); i++){
				mIdMap.put(objects.get(i), i);
			}
		}
		@Override 
		public long getItemId(int position){
			String item = getItem(position);
			return mIdMap.get(item);
		}
		@Override
		public boolean hasStableIds(){
			return true;
		}
		
	}
	@Override 
	public void onResume(){
		super.onResume();
	}
	@Override
	public void onPause(){
		super.onPause();
		Log.d("BLUE", "pause ");
		if(outstream != null){
			try{
				outstream.flush();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		try {
			bsocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void sendData(String message){
		byte[] msgBuffer = message.getBytes();
		Log.d("BLUE", "send data "+message);
		try {
			outstream.write(msgBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override 
	public void onActivityResult(int req, int res, Intent data){
		if(req == ena){
			BlueActivity.this.finish();
		}
	}
}
