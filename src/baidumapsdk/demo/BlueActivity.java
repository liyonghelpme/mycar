package baidumapsdk.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.liyong.map.R;

import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class BlueActivity extends Activity {
	final Context con = this;
	BlueActivity bact = this;
	Button scan;
	Set<BluetoothDevice> pairedDevices;
	BluetoothAdapter mBluetoothAdapter;
	List<String> mdevice;
	ListView lv;
	StableArrayAdapter adapter;
	private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	BluetoothSocket bsocket;
	//OutputStream outstream = null;
	AlertDialog.Builder b;
	public int ena = 1;
	String conAddress;
	final int RECEIVE_MESSAGE = 1;
	final int CONNECT = 2;
	private ConnectedThread mConnectedThread;
	Handler h;
	TextView txtArduino;
	
	private StringBuilder sb = new StringBuilder();
	
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
			lv.setVisibility(View.VISIBLE);
		}
		
	};
	@Override
	public void onCreate(Bundle saved){
		super.onCreate(saved);
		setContentView(R.layout.blue);
		initPopUp();
		txtArduino = (TextView)findViewById(R.id.editText1);
		h = new Handler(){
			@SuppressLint("NewApi")
			public void handleMessage(Message msg){
				switch(msg.what){
				case RECEIVE_MESSAGE:
					 byte[] readBuf = (byte[]) msg.obj;
		                String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
		                sb.append(strIncom);                                                // append string
		                int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
		                if (endOfLineIndex > 0) {                                            // if end-of-line,
		                    String sbprint = sb.substring(0, endOfLineIndex);               // extract string
		                    sb.delete(0, sb.length());                                      // and clear
		                    txtArduino.setText("Data from Arduino: " + sbprint);            // update TextView
		                }
		                //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
		                break;
				case CONNECT:
					 if(bsocket != null){
						 //连接成功
						 mConnectedThread = new ConnectedThread(bsocket);
						 mConnectedThread.start();
						 //bact.getActionBar().setTitle("连接成功");
						 bact.setTitle("连接成功");
					 }else{
						 //连接失败
						 b = new AlertDialog.Builder(con);
							b.setMessage("无法连接到该设备!!")
							.setPositiveButton("确定", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									//BlueActivity.this.finish();
									dialog.cancel();
								}
							});
						AlertDialog a = b.create();
						a.show();
						//bact.getActionBar().setTitle("连接失败！");
						bact.setTitle("连接失败");
					 }
				}
			}
		};
		
		//注册搜索蓝牙设备 成功后  发送消息的监听器
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		mdevice = new ArrayList<String>();
		lv = (ListView)findViewById(R.id.listView1);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//检测手机是否支持蓝牙
		if(mBluetoothAdapter == null){
			b = new AlertDialog.Builder(this);
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
	private class ConnectThread extends Thread {
		public final BluetoothSocket mmsocket;
		public final BluetoothDevice device;
		public ConnectThread(BluetoothDevice d){
			BluetoothSocket tmp = null;
			device = d;
			try{
				tmp = device.createRfcommSocketToServiceRecord(myUUID);
			}catch(IOException e){
				e.printStackTrace();
			}
			mmsocket = tmp;
		}
		public void run(){
			mBluetoothAdapter.cancelDiscovery();
			try{
				mmsocket.connect();
			}catch(Exception e){
				e.printStackTrace();
				try {
					mmsocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				bsocket = null;
				h.obtainMessage(CONNECT, 0, 0, null).sendToTarget();
				return;
			}
			bsocket = mmsocket;
			h.obtainMessage(CONNECT, 0, 0, null).sendToTarget();
		}
	}
	public void setupText(){
		
	}
	public ConnectThread cth;
	//开始连接设备
	public void initBluetooth(){
		//蓝牙未开启 提示用户开启蓝牙设备
		if(!mBluetoothAdapter.isEnabled()){
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, ena);
		
		} else {
			//显示已经配对的设备
			pairedDevices = mBluetoothAdapter.getBondedDevices();
			if(pairedDevices.size() > 0){
				for(BluetoothDevice device : pairedDevices){
					mdevice.add(device.getName()+"\n"+device.getAddress());
				}
			}
			adapter = new StableArrayAdapter(con, android.R.layout.simple_list_item_1, mdevice);
			lv.setAdapter(adapter);
			//连接选择的设备
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@SuppressLint("NewApi")
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					final String item = (String)parent.getItemAtPosition(position);
					Log.i("BLUE", "touch item "+item);
					String[] val = item.split("\n");
					Log.i("BLUE", val[1]);
					conAddress = val[1];
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(val[1]);
					cth = new ConnectThread(device);
					cth.start();
					Log.d("BLUE", "...create socket");
					lv.setVisibility(View.INVISIBLE);
					//bact.getActionBar().setTitle("正在连接");
					bact.setTitle("正在连接");
				}
				
			});
			
			//扫描设备
			scan = (Button)findViewById(R.id.button1);
			scan.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View arg0) {
					boolean sd = mBluetoothAdapter.startDiscovery();
					Log.i("BLUE", "start scan");
				}
			});
			
			Button on = (Button)findViewById(R.id.button2);
			on.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					/*
					if(outstream == null){
						b = new AlertDialog.Builder(con);
						b.setMessage("还没有连接上设备!!")
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
					*/	
						//sendData("1");
						mConnectedThread.write("1");
					//}
				}
				
			});
			Button off = (Button)findViewById(R.id.button3);
			off.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					/*
					if(outstream == null){
						b = new AlertDialog.Builder(con);
						b.setMessage("还没有连接上设备!!")
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
					*/
						//sendData("0");
						mConnectedThread.write("0");
					//}
				}
				
			});
		}
	}
	private class ConnectedThread extends Thread{
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		public ConnectedThread(BluetoothSocket socket){
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch(IOException e){
				
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		public void run(){
			byte[] buffer = new byte[256];
			int bytes;
			while(true){
				try{
					bytes = mmInStream.read(buffer);
					h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
				}catch(IOException e){
					break;
				}
			}
		}
		public void write(String message){
			
			Log.d("BLUE", ".. Data to send: "+message+"...");
			byte[] msgBuffer = message.getBytes();
			try{
				mmOutStream.write(msgBuffer);
			}catch (IOException e){
				 Log.d("BLUE", "Error data send "+e.getMessage());
			}
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
		/*
		if(conAddress != null) {
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(conAddress);
			try {
				bsocket = createBluetoothSocket(device);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				BlueActivity.this.finish();
			}
			mBluetoothAdapter.cancelDiscovery();
			Log.d("BLUE", "connecting..."+conAddress);
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
		*/
	}
	@Override
	public void onPause(){
		super.onPause();
		Log.d("BLUE", "pause ");
		/*
		if(outstream != null){
			try{
				outstream.flush();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		*/
		try {
			//bsocket.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
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
	*/
	@Override 
	public void onActivityResult(int req, int res, Intent data){
		if(req == ena){
			BlueActivity.this.finish();
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inf = getMenuInflater();
		inf.inflate(R.menu.mymenu, menu);
		return true;
	}
	public LinearLayout ll;
	PopupWindow pw;
	public void initPopUp(){
		TextView pv = new TextView(this);
		pv.setText("pop up");
		ll = new LinearLayout(this);
		ll.setOrientation(1);
		ll.addView(pv);
		pw = new PopupWindow(ll, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		pw.setContentView(ll);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.connectDev:
			final CharSequence [] items = {
				"what\ngo now", "love\ndream"	
			};
			//pw.showAsDropDown(txtArduino);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("选择设备");
			/*
			builder.setItems(items, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			});
			*/
			//AlertDialog a = builder.create();
			LayoutInflater inflater = getLayoutInflater();
			//FrameLayout f1 = (FrameLayout)a.findViewById(android.R.id.body);
			
			View v = inflater.inflate(R.layout.mydia, null);
			builder.setView(v);
			builder.show();
			//a.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
		
	}
}
