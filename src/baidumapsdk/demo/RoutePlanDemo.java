package baidumapsdk.demo;


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKEvent;
import com.baidu.mapapi.map.MKMapTouchListener;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.PopupClickListener;
import com.baidu.mapapi.map.PopupOverlay;
import com.baidu.mapapi.map.RouteOverlay;
import com.baidu.mapapi.map.TransitOverlay;
import com.baidu.mapapi.search.MKAddrInfo;
import com.baidu.mapapi.search.MKBusLineResult;
import com.baidu.mapapi.search.MKDrivingRouteResult;
import com.baidu.mapapi.search.MKGeocoderAddressComponent;
import com.baidu.mapapi.search.MKPlanNode;
import com.baidu.mapapi.search.MKPoiResult;
import com.baidu.mapapi.search.MKRoute;
import com.baidu.mapapi.search.MKRoutePlan;
import com.baidu.mapapi.search.MKSearch;
import com.baidu.mapapi.search.MKSearchListener;
import com.baidu.mapapi.search.MKShareUrlResult;
import com.baidu.mapapi.search.MKStep;
import com.baidu.mapapi.search.MKSuggestionResult;
import com.baidu.mapapi.search.MKTransitRouteResult;
import com.baidu.mapapi.search.MKWalkingRouteResult;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.liyong.map.R;
/**
 * 此demo用来展示如何进行驾车、步行、公交路线搜索并在地图使用RouteOverlay、TransitOverlay绘制
 * 同时展示如何进行节点浏览并弹出泡泡
 *
 */
public class RoutePlanDemo extends Activity {
	public BlueActivity bact;
	RoutePlanDemo demo = this;
	//UI相关
	Button mBtnDrive = null;	// 驾车搜索
	Button mBtnTransit = null;	// 公交搜索
	Button mBtnWalk = null;	// 步行搜索
	Button mBtnCusRoute = null; //自定义路线
	Button mBtnCusIcon = null ; //自定义起终点图标
	
	//浏览路线节点相关
	Button mBtnPre = null;//上一个节点
	Button mBtnNext = null;//下一个节点
	int nodeIndex = -2;//节点索引,供浏览节点时使用
	MKRoute route = null;//保存驾车/步行路线数据的变量，供浏览节点时使用
	TransitOverlay transitOverlay = null;//保存公交路线图层数据的变量，供浏览节点时使用
	RouteOverlay routeOverlay = null; 
	boolean useDefaultIcon = false;
	int searchType = -1;//记录搜索的类型，区分驾车/步行和公交
	private PopupOverlay   pop  = null;//弹出泡泡图层，浏览节点时使用
	private TextView  popupText = null;//泡泡view
	private View viewCache = null;
	
	//地图相关，使用继承MapView的MyRouteMapView目的是重写touch事件实现泡泡处理
	//如果不处理touch事件，则无需继承，直接使用MapView即可
	MapView mMapView = null;	// 地图View
	//搜索相关
	MKSearch mSearch = null;	// 搜索模块，也可去掉地图模块独立使用
	
	private Activity act;
	GPSTracker gps;
	LocationManager locationm;
	Button mcityBut;
	String myCity;
	MyLocationOverlay myLocationOverlay;
	TextView startPos;
	TextView endPos;
	
	boolean searchCity = false;
	boolean searchSuc = true;
	
	boolean searchPathYet = false;
	boolean errorInPath = false;
	MKRoute myRoute;
	List<GeoPoint> allPoints;
	int curPoint;
	public TextView blueInfo;
	Button dir;
	Button forward;
	Button stop;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		act = this;
		Intent myIntent = getIntent();
		bact = BlueActivity.globalBact;
		if(bact.mConnectedThread != null) {
			bact.mConnectedThread.pd = this;
		}
		
        DemoApplication app = (DemoApplication)this.getApplication();
		setContentView(R.layout.routeplan);
		CharSequence titleLable="路线规划功能";
        setTitle(titleLable);
        
        gps = new GPSTracker(this);
        blueInfo = (TextView)findViewById(R.id.editText1);
        blueInfo.setClickable(false);
        blueInfo.setFocusable(false);
        
        dir = (Button)findViewById(R.id.button1);
        dir.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(rth != null) {
					Log.d("MAP", "CURDIR"+rth.curDir);
					if(bact.mConnectedThread != null) {
						bact.mConnectedThread.write("N"+rth.curDir+"\n");
					}
				}
			}
        	
        });
        forward = (Button)findViewById(R.id.button2);
		forward.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(bact.mConnectedThread != null) {
					bact.mConnectedThread.write("f");
				}
			}
			
		});
		stop = (Button)findViewById(R.id.button3);
		stop.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(bact.mConnectedThread != null) {
					bact.mConnectedThread.write("s");
				}
			}
			
		});
        //初始化地图
        mMapView = (MapView)findViewById(R.id.bmapView);
        mMapView.setBuiltInZoomControls(false);
        mMapView.getController().setZoom(12);
        mMapView.getController().enableClick(true);
        
        startPos = (TextView)findViewById(R.id.start);
        startPos.setFocusable(false);
        startPos.setClickable(false);
        startPos.setText("我的位置");
        
        endPos = (TextView)findViewById(R.id.end);
        endPos.setText("碧水豪园");
        //初始化按键
        mBtnDrive = (Button)findViewById(R.id.drive);
        mBtnDrive.setVisibility(View.INVISIBLE);
        
        mBtnTransit = (Button)findViewById(R.id.transit);
        mBtnTransit.setVisibility(View.INVISIBLE);
        
        mBtnWalk = (Button)findViewById(R.id.walk);
        mBtnPre = (Button)findViewById(R.id.pre);
        mBtnNext = (Button)findViewById(R.id.next);
        mBtnCusRoute = (Button)findViewById(R.id.custombutton);
        mBtnCusIcon = (Button)findViewById(R.id.customicon);
        mBtnPre.setVisibility(View.INVISIBLE);
		mBtnNext.setVisibility(View.INVISIBLE);
	    mcityBut = (Button)findViewById(R.id.findCity);
	    mBtnWalk.setVisibility(View.INVISIBLE);
        //按键点击事件
        OnClickListener clickListener = new OnClickListener(){
			public void onClick(View v) {
				//发起搜索
				SearchButtonProcess(v);
			}
        };
        OnClickListener nodeClickListener = new OnClickListener(){
			public void onClick(View v) {
				//浏览路线节点
				nodeClick(v);
			}
        };
        OnClickListener customClickListener = new OnClickListener(){
			public void onClick(View v) {
				//自设路线绘制示例
				intentToActivity();
				
			}
        };
        
        OnClickListener changeRouteIconListener = new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				changeRouteIcon();
			}
        	
        };
        
        mBtnDrive.setOnClickListener(clickListener); 
        mBtnTransit.setOnClickListener(clickListener); 
        mBtnWalk.setOnClickListener(clickListener);
        mBtnPre.setOnClickListener(nodeClickListener);
        mBtnNext.setOnClickListener(nodeClickListener);
        mBtnCusRoute.setOnClickListener(customClickListener);
        mBtnCusIcon.setOnClickListener(changeRouteIconListener);
        //创建 弹出泡泡图层
        createPaopao();
       
        //地图点击事件处理
        mMapView.regMapTouchListner(new MKMapTouchListener(){

			@Override
			public void onMapClick(GeoPoint point) {
			  //在此处理地图点击事件 
			  //消隐pop
			  if ( pop != null ){
				  pop.hidePop();
			  }
			}

			@Override
			public void onMapDoubleClick(GeoPoint point) {
				
			}

			@Override
			public void onMapLongClick(GeoPoint point) {
				
			}
        	
        });
        // 初始化搜索模块，注册事件监听
        mSearch = new MKSearch();
        mSearch.init(app.mBMapManager, new MKSearchListener(){

			public void onGetDrivingRouteResult(MKDrivingRouteResult res,
					int error) {
				//起点或终点有歧义，需要选择具体的城市列表或地址列表
				if (error == MKEvent.ERROR_ROUTE_ADDR){
					//遍历所有地址
//					ArrayList<MKPoiInfo> stPois = res.getAddrResult().mStartPoiList;
//					ArrayList<MKPoiInfo> enPois = res.getAddrResult().mEndPoiList;
//					ArrayList<MKCityListInfo> stCities = res.getAddrResult().mStartCityList;
//					ArrayList<MKCityListInfo> enCities = res.getAddrResult().mEndCityList;
					return;
				}
				// 错误号可参考MKEvent中的定义
				if (error != 0 || res == null) {
					Toast.makeText(RoutePlanDemo.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
					return;
				}
			
				searchType = 0;
			    routeOverlay = new RouteOverlay(RoutePlanDemo.this, mMapView);
			    // 此处仅展示一个方案作为示例
			    routeOverlay.setData(res.getPlan(0).getRoute(0));
			    //清除其他图层
			    mMapView.getOverlays().clear();
			    //添加路线图层
			    mMapView.getOverlays().add(routeOverlay);
			    //执行刷新使生效
			    mMapView.refresh();
			    // 使用zoomToSpan()绽放地图，使路线能完全显示在地图上
			    mMapView.getController().zoomToSpan(routeOverlay.getLatSpanE6(), routeOverlay.getLonSpanE6());
			    //移动地图到起点
			    mMapView.getController().animateTo(res.getStart().pt);
			    //将路线数据保存给全局变量
			    route = res.getPlan(0).getRoute(0);
			    //重置路线节点索引，节点浏览时使用
			    nodeIndex = -1;
			    mBtnPre.setVisibility(View.VISIBLE);
				mBtnNext.setVisibility(View.VISIBLE);
			}

			public void onGetTransitRouteResult(MKTransitRouteResult res,
					int error) {
				//起点或终点有歧义，需要选择具体的城市列表或地址列表
				if (error == MKEvent.ERROR_ROUTE_ADDR){
					//遍历所有地址
//					ArrayList<MKPoiInfo> stPois = res.getAddrResult().mStartPoiList;
//					ArrayList<MKPoiInfo> enPois = res.getAddrResult().mEndPoiList;
//					ArrayList<MKCityListInfo> stCities = res.getAddrResult().mStartCityList;
//					ArrayList<MKCityListInfo> enCities = res.getAddrResult().mEndCityList;
					return;
				}
				if (error != 0 || res == null) {
					Toast.makeText(RoutePlanDemo.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
					return;
				}
				
				searchType = 1;
				transitOverlay = new TransitOverlay (RoutePlanDemo.this, mMapView);
			    // 此处仅展示一个方案作为示例
			    transitOverlay.setData(res.getPlan(0));
			  //清除其他图层
			    mMapView.getOverlays().clear();
			  //添加路线图层
			    mMapView.getOverlays().add(transitOverlay);
			  //执行刷新使生效
			    mMapView.refresh();
			    // 使用zoomToSpan()绽放地图，使路线能完全显示在地图上
			    mMapView.getController().zoomToSpan(transitOverlay.getLatSpanE6(), transitOverlay.getLonSpanE6());
			  //移动地图到起点
			    mMapView.getController().animateTo(res.getStart().pt);
			  //重置路线节点索引，节点浏览时使用
			    nodeIndex = 0;
			    mBtnPre.setVisibility(View.VISIBLE);
				mBtnNext.setVisibility(View.VISIBLE);
			}
			
			//得到路径信息 通知机器人 往那里走啦！！
			//起点终点信息 
			//方案数量
			public void onGetWalkingRouteResult(MKWalkingRouteResult res,
					int error) {
				demo.setTitle("路径搜索结束");
				searchPathYet = true;
				errorInPath = false;
				//起点或终点有歧义，需要选择具体的城市列表或地址列表
				if (error == MKEvent.ERROR_ROUTE_ADDR){
					errorInPath = true;
					//遍历所有地址
//					ArrayList<MKPoiInfo> stPois = res.getAddrResult().mStartPoiList;
//					ArrayList<MKPoiInfo> enPois = res.getAddrResult().mEndPoiList;
//					ArrayList<MKCityListInfo> stCities = res.getAddrResult().mStartCityList;
//					ArrayList<MKCityListInfo> enCities = res.getAddrResult().mEndCityList;
					return;
				}
				if (error != 0 || res == null) {
					Toast.makeText(RoutePlanDemo.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
					return;
				}
				Toast.makeText(RoutePlanDemo.this, "成功找到路径", Toast.LENGTH_SHORT).show();
				Log.v("MAP", "number "+res.getNumPlan());
				   double lat = gps.getLatitude();
				   double lon = gps.getLongitude();
				   Log.v("MAP", "my position "+lat+" "+lon);
			       //检测是否允许gps 定位！
			
			   
			    
				
				if(res.getNumPlan() > 0) {
					MKRoutePlan p = res.getPlan(0);
					Log.v("MAP", "distance "+p.getDistance());
					Log.v("MAP", "routes "+p.getNumRoutes());
					Log.v("MAP", "time "+p.getTime());
					if(p.getNumRoutes() > 0) {
						MKRoute r = p.getRoute(0);
						myRoute = r;
						curPoint = 0;
						allPoints = new ArrayList<GeoPoint>();
						
						Log.v("MAP", "step num "+r.getNumSteps());
						Log.v("MAP", "route type "+r.getRouteType());
						ArrayList<ArrayList<GeoPoint> > arr = r.getArrayPoints();
						int lastLat = 0;
						int lastLon = 0;
						for(int i = 0; i < r.getNumSteps(); i++) {
							MKStep s = r.getStep(i);
							Log.v("MAP", "map content "+s.getContent());
							//根据当前所在位置 计算 下一个位置的点  check 一下当前位置点 
							GeoPoint gp = s.getPoint();
							int la = gp.getLatitudeE6();
							int lo = gp.getLongitudeE6();
							allPoints.add(gp);
							Log.v("MAP", "weidu "+gp);
							//计算方向
							if(i > 0 ){
								
								double lat1 = lastLat/1000000.*Math.PI/180;
								double lon1 = lastLon/1000000.*Math.PI/180;
								double lat2 = la*1.0/1000000*Math.PI/180;
								double lon2 = lo*1.0/1000000*Math.PI/180;
								double dLon = lon2-lon1;
								double dlat = lat2-lat1;
								double y = Math.sin(dLon) * Math.cos(lat2);
								double x = Math.cos(lat1)*Math.sin(lat2) -
								        Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
								double brng = Math.atan2(y, x)*180/Math.PI;
								Log.v("Map", "diff lat, diff lon "+dlat+" "+dLon);
								Log.v("MAP", "bearing "+brng);
								if(brng >= 0 && brng < 90) {
									Log.v("MAP", "东北方向走起");
								}else if(brng >= 90 && brng < 270) {
									
								}
								
							}
							lastLat = la;
							lastLon = lo;
						}
						rth = new RouteThread();
						rth.start();
					}
				}
				searchType = 2;
				routeOverlay = new RouteOverlay(RoutePlanDemo.this, mMapView);
			    // 此处仅展示一个方案作为示例
				routeOverlay.setData(res.getPlan(0).getRoute(0));
				//清除其他图层
			    mMapView.getOverlays().clear();
			  //添加路线图层
			    mMapView.getOverlays().add(routeOverlay);
			  //执行刷新使生效
			    mMapView.refresh();
			    // 使用zoomToSpan()绽放地图，使路线能完全显示在地图上
			    mMapView.getController().zoomToSpan(routeOverlay.getLatSpanE6(), routeOverlay.getLonSpanE6());
			  //移动地图到起点
			    mMapView.getController().animateTo(res.getStart().pt);
			    //将路线数据保存给全局变量
			    route = res.getPlan(0).getRoute(0);
			    //重置路线节点索引，节点浏览时使用
			    nodeIndex = -1;
			    mBtnPre.setVisibility(View.VISIBLE);
				mBtnNext.setVisibility(View.VISIBLE);
			    
				LocationData locData = new LocationData();
				locData.latitude = gps.getLatitude();
				locData.longitude = gps.getLongitude();
				locData.direction = 0f;
				myLocationOverlay.setData(locData);
				
				
				mMapView.getOverlays().add(myLocationOverlay);
				mMapView.refresh();
				mMapView.getController().animateTo(new GeoPoint((int)(locData.latitude*1e6), (int)(locData.longitude*1e6)));
			}
			public void onGetAddrResult(MKAddrInfo res, int error) {
				MKGeocoderAddressComponent kk = res.addressComponents;
				String city = kk.city;
				Log.d("BLUE", "my city "+city);
				demo.setTitle("你的城市是 "+city);
				myCity = city;
				mBtnWalk.setVisibility(View.VISIBLE);
				
				if(myLocationOverlay == null)
					myLocationOverlay = new MyLocationOverlay(mMapView);
				LocationData locData = new LocationData();
				locData.latitude = gps.getLatitude();
				locData.longitude = gps.getLongitude();
				locData.direction = 0f;
				myLocationOverlay.setData(locData);
				
				mMapView.getOverlays().clear();
				mMapView.getOverlays().add(myLocationOverlay);
				mMapView.refresh();
				mMapView.getController().animateTo(new GeoPoint((int)(locData.latitude*1e6), (int)(locData.longitude*1e6)));
			
				searchCity = false;
				if(myCity == ""){
					searchSuc = false;
				} else {
					searchSuc = true;
				}
			}
			public void onGetPoiResult(MKPoiResult res, int arg1, int arg2) {
			}
			public void onGetBusDetailResult(MKBusLineResult result, int iError) {
			}

			@Override
			public void onGetSuggestionResult(MKSuggestionResult res, int arg1) {
			}

			@Override
			public void onGetPoiDetailSearchResult(int type, int iError) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onGetShareUrlResult(MKShareUrlResult result, int type,
					int error) {
				// TODO Auto-generated method stub
				
			}
        });
        
        Log.d("BLUE", "get User position");
		if(!gps.canGetLocation()) {
			gps.showSettingsAlert();
		} else {
			Log.d("BLUE", "user pos "+gps.getLatitude()+" "+gps.getLongitude());
			//mSearch.reverseGeocode(new GeoPoint((int)(gps.getLatitude()*1e6), (int)(gps.getLongitude()*1e6)));
			mcityBut.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					startSearchCity();
				}
		    	
		    });
		}
		
	}
	RouteThread rth;
	boolean getTarget = false;
	public class RouteThread extends Thread {
		public int state = 0;
		public int curDir = 0;
		public long waitTime = 0;
		public RouteThread(){
			
		}
		public void showInfo(final String w){
			demo.runOnUiThread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Toast.makeText(demo, w, Toast.LENGTH_SHORT).show();
				}
				
			});
		}
		@Override
		public void run(){
			while(true){
				if(curPoint >= allPoints.size()) {
					getTarget = true;
					showInfo("到达目的地");
					break;
				}
				GeoPoint t1 = allPoints.get(curPoint);
				int lat = (int)(gps.getLatitude()*1e6);
				int lon = (int)(gps.getLongitude()*1e6);
				GeoPoint t2 = new GeoPoint(lat, lon);
				final double dist = DistanceUtil.getDistance(t1, t2);
				if(dist <= 10){
					curPoint = curPoint +1;
					continue;
				}
				Log.v("MAP", "距离下一个目标点 距离是 "+dist);
				demo.runOnUiThread(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Toast.makeText(demo, "距离下一个目标点距离是 "+(int)dist+"米", Toast.LENGTH_SHORT).show();
					
						LocationData locData = new LocationData();
						locData.latitude = gps.getLatitude();
						locData.longitude = gps.getLongitude();
						locData.direction = curDir;
						myLocationOverlay.setData(locData);
						
						myLocationOverlay.enableCompass();
						
						mMapView.refresh();
					}
					
				});
				//小车转向
				if(state == 0){
					
					double lat1 = lat/1000000.*Math.PI/180;
					double lon1 = lon/1000000.*Math.PI/180;
					double lat2 = t1.getLatitudeE6()*1.0/1000000*Math.PI/180;
					double lon2 = t1.getLongitudeE6()*1.0/1000000*Math.PI/180;
					double dLon = lon2-lon1;
					double dlat = lat2-lat1;
					double y = Math.sin(dLon) * Math.cos(lat2);
					double x = Math.cos(lat1)*Math.sin(lat2) -
					        Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
					double brng = Math.atan2(y, x)*180/Math.PI;
					if(brng < 0){
						brng += 360;
					}
					curDir = (int)brng;
					Log.v("Map", "diff lat, diff lon "+dlat+" "+dLon);
					Log.v("MAP", "bearing "+brng);
					
				//命令 向北 多少度 旋转  Nxxx\n
				//DIROK\n
				//f  forward 小车行动10s种
				//MOVEOK\n
				//人行动一会 追上小车 接着行动
					
					if(bact != null) {
						state = 1;
						Log.d("Map", "send CMD");
						if(brng < 0){
							brng += 360;
						}
						if(bact.mConnectedThread != null) {
							bact.mConnectedThread.write("N"+(int)(brng)+"\n");
						}
					}
				//读取蓝牙数据
				} else if(state == 1) {
					String cmd = null;
					String [] allcmd;
					int last = 0;
					synchronized("cmdbuffer"){
						allcmd = bact.cmdBuffer.split("\n");
						last = bact.cmdBuffer.lastIndexOf("\n");
					}
					int i = 0;
					for(; i < allcmd.length; i++){
						cmd = allcmd[i];
						Log.d("BLUE", "read cmd is "+cmd);
						if(allcmd[i].equals("DIROK")){
							waitTime = System.currentTimeMillis();
							state = 2;
							if(bact.mConnectedThread != null) {
								bact.mConnectedThread.write("f");
							}
							showInfo("小车方向旋转结束");
							break;
						}
					}
					synchronized("cmdbuffer"){
						bact.cmdBuffer = bact.cmdBuffer.substring(last+1);
					}
					
					Log.d("MAP", "readCMD");
					Log.d("MAP", cmd);
					
				}else if(state == 2) {
					
					long now = System.currentTimeMillis();
					if(now - waitTime > 3*1000) {
						if(bact.mConnectedThread != null) {
							bact.mConnectedThread.write("s");
						}
						state = 3;
						waitTime = now;
					}
					
				}else if(state == 3){
					long now = System.currentTimeMillis();
					if(now-waitTime > 5*1000) {
					//如果用户和 小车距离足够近了 则 回到0 状态
				
						state = 0;
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	public void startSearchCity(){
		searchCity = true;
		demo.setTitle("定位当前城市。。。");
		Log.d("BLUE", "user pos "+gps.getLatitude()+" "+gps.getLongitude());
		mSearch.reverseGeocode(new GeoPoint((int)(gps.getLatitude()*1e6), (int)(gps.getLongitude()*1e6)));
	}
	/**
	 * 发起路线规划搜索示例
	 * @param v
	 */
	void SearchButtonProcess(View v) {
		demo.setTitle("正在搜索。。。");
		//重置浏览节点的路线数据
		route = null;
		routeOverlay = null;
		transitOverlay = null; 
		mBtnPre.setVisibility(View.INVISIBLE);
		mBtnNext.setVisibility(View.INVISIBLE);
		// 处理搜索按钮响应
		EditText editSt = (EditText)findViewById(R.id.start);
		EditText editEn = (EditText)findViewById(R.id.end);
		
		// 对起点终点的name进行赋值，也可以直接对坐标赋值，赋值坐标则将根据坐标进行搜索
		MKPlanNode stNode = new MKPlanNode();
		//stNode.name = editSt.getText().toString();
		stNode.pt = new GeoPoint((int)(gps.getLatitude()*1e6), (int)(gps.getLongitude()*1e6));
		MKPlanNode enNode = new MKPlanNode();
		enNode.name = editEn.getText().toString();

		// 实际使用中请对起点终点城市进行正确的设定
		if (mBtnDrive.equals(v)) {
			mSearch.drivingSearch("北京", stNode, "北京", enNode);
		} else if (mBtnTransit.equals(v)) {
			mSearch.transitSearch("北京", stNode, enNode);
		} else if (mBtnWalk.equals(v)) {
			mSearch.walkingSearch(myCity, stNode, myCity, enNode);
		} 
	}
	/**
	 * 节点浏览示例
	 * @param v
	 */
	public void nodeClick(View v){
		viewCache = getLayoutInflater().inflate(R.layout.custom_text_view, null);
        popupText =(TextView) viewCache.findViewById(R.id.textcache);
		if (searchType == 0 || searchType == 2){
			//驾车、步行使用的数据结构相同，因此类型为驾车或步行，节点浏览方法相同
			if (nodeIndex < -1 || route == null || nodeIndex >= route.getNumSteps())
				return;
			
			//上一个节点
			if (mBtnPre.equals(v) && nodeIndex > 0){
				//索引减
				nodeIndex--;
				//移动到指定索引的坐标
				mMapView.getController().animateTo(route.getStep(nodeIndex).getPoint());
				//弹出泡泡
				popupText.setBackgroundResource(R.drawable.popup);
				popupText.setText(route.getStep(nodeIndex).getContent());
				pop.showPopup(BMapUtil.getBitmapFromView(popupText),
						route.getStep(nodeIndex).getPoint(),
						5);
			}
			//下一个节点
			if (mBtnNext.equals(v) && nodeIndex < (route.getNumSteps()-1)){
				//索引加
				nodeIndex++;
				//移动到指定索引的坐标
				mMapView.getController().animateTo(route.getStep(nodeIndex).getPoint());
				//弹出泡泡
				popupText.setBackgroundResource(R.drawable.popup);
				popupText.setText(route.getStep(nodeIndex).getContent());
				pop.showPopup(BMapUtil.getBitmapFromView(popupText),
						route.getStep(nodeIndex).getPoint(),
						5);
			}
		}
		if (searchType == 1){
			//公交换乘使用的数据结构与其他不同，因此单独处理节点浏览
			if (nodeIndex < -1 || transitOverlay == null || nodeIndex >= transitOverlay.getAllItem().size())
				return;
			
			//上一个节点
			if (mBtnPre.equals(v) && nodeIndex > 1){
				//索引减
				nodeIndex--;
				//移动到指定索引的坐标
				mMapView.getController().animateTo(transitOverlay.getItem(nodeIndex).getPoint());
				//弹出泡泡
				popupText.setBackgroundResource(R.drawable.popup);
				popupText.setText(transitOverlay.getItem(nodeIndex).getTitle());
				pop.showPopup(BMapUtil.getBitmapFromView(popupText),
						transitOverlay.getItem(nodeIndex).getPoint(),
						5);
			}
			//下一个节点
			if (mBtnNext.equals(v) && nodeIndex < (transitOverlay.getAllItem().size()-2)){
				//索引加
				nodeIndex++;
				//移动到指定索引的坐标
				mMapView.getController().animateTo(transitOverlay.getItem(nodeIndex).getPoint());
				//弹出泡泡
				popupText.setBackgroundResource(R.drawable.popup);
				popupText.setText(transitOverlay.getItem(nodeIndex).getTitle());
				pop.showPopup(BMapUtil.getBitmapFromView(popupText),
						transitOverlay.getItem(nodeIndex).getPoint(),
						5);
			}
		}
		
	}
	/**
	 * 创建弹出泡泡图层
	 */
	public void createPaopao(){
		
        //泡泡点击响应回调
        PopupClickListener popListener = new PopupClickListener(){
			@Override
			public void onClickedPopup(int index) {
				Log.v("click", "clickapoapo");
			}
        };
        pop = new PopupOverlay(mMapView,popListener);
	}
	/**
	 * 跳转自设路线Activity
	 */
	public void intentToActivity(){
		//跳转到自设路线演示demo
		Intent intent = new Intent(this, CustomRouteOverlayDemo.class);
    	startActivity(intent); 
	}
	
	/**
	 * 切换路线图标，刷新地图使其生效
	 * 注意： 起终点图标使用中心对齐.
	 */
	protected void changeRouteIcon() {
	    Button btn = (Button)findViewById(R.id.customicon);
	    if ( routeOverlay == null && transitOverlay == null){
	    	return ;
	    }
		if ( useDefaultIcon ){
	    	if ( routeOverlay != null){
	    	    routeOverlay.setStMarker(null);
	    	    routeOverlay.setEnMarker(null);
	        }
	        if ( transitOverlay != null){
	    	    transitOverlay.setStMarker(null);
	    	    transitOverlay.setEnMarker(null);
	        }
	        btn.setText("自定义起终点图标");
	        Toast.makeText(this, 
	        		       "将使用系统起终点图标", 
	        		       Toast.LENGTH_SHORT).show();
	    }
	    else{
		    if ( routeOverlay != null){
	    	    routeOverlay.setStMarker(getResources().getDrawable(R.drawable.icon_st));
	    	    routeOverlay.setEnMarker(getResources().getDrawable(R.drawable.icon_en));
	        }
	        if ( transitOverlay != null){
	    	    transitOverlay.setStMarker(getResources().getDrawable(R.drawable.icon_st));
	    	    transitOverlay.setEnMarker(getResources().getDrawable(R.drawable.icon_en));
	        }
	        btn.setText("系统起终点图标");
	        Toast.makeText(this, 
	        		       "将使用自定义起终点图标", 
	        		       Toast.LENGTH_SHORT).show();
	    }
	    useDefaultIcon = !useDefaultIcon;
	    mMapView.refresh();
		
	}

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }
    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        mMapView.destroy();
        super.onDestroy();
    }
	
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	mMapView.onSaveInstanceState(outState);
    	
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	mMapView.onRestoreInstanceState(savedInstanceState);
    }
	public void updateLocation() {
		if(searchCity == false && searchSuc == false){
			startSearchCity();
		}
	}
}
