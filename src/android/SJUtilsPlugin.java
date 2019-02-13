package com.fhsjdz.cordova.utils;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.fhsjdz.cordova.utils.WifiAdmin;
import com.fhsjdz.cordova.utils.update.UpdateManager;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SJUtilsPlugin extends CordovaPlugin {
	
	private JSONArray requestArgs;
    private CallbackContext callbackContext;
	private UpdateManager updateManager = null;
	private static String TAG = "SJUtilsPlugin";
	private IEsptouchTask mEsptouchTask;
	private DownloadManager downloadManager;
	private long mTaskId;
	private RelativeLayout toastLayout = null;
	private TextView toastView = null;
	private String [] updatePerms = { Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private String [] wifiPerms = { Manifest.permission.ACCESS_FINE_LOCATION };
	
	private static final String GET_PREFERRED_LANGUAGE = "getPreferredLanguage";
    private static final String CONNECT_WIFI = "connectWiFi";
    private static final String GET_WIFI_SSID = "getWifiSSID";
	
	//listener to get result
    private IEsptouchListener myListener = new IEsptouchListener(){
        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            String text = "bssid:"+ result.getBssid()+",InetAddress:"+result.getInetAddress().getHostAddress() + " is connected to the wifi";
            Log.d(TAG, text);
        }
    };
    
	//广播接受者，接收下载状态
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkDownloadStatus();//检查下载状态
        }
    };
    
	/**
     * Constructor.
     */
    public SJUtilsPlugin() {}
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext){
    	try {
	    	if (action.equals("getVersionCode")) {
	    		getVersionCode(args, callbackContext);
	    	}else if(action.equals("update")){
	    		if(!hasPermisssion(updatePerms)) {
	    			this.requestArgs = args;
	    			this.callbackContext = callbackContext;
	    			PermissionHelper.requestPermissions(this, 0, updatePerms);
	            } else {
	            	update(args, callbackContext);
	            }
	    	}else if(action.equals(GET_WIFI_SSID)){
	    		if(!hasPermisssion(wifiPerms)) {
                    this.requestArgs = args;
                    this.callbackContext = callbackContext;
                    PermissionHelper.requestPermissions(this, 1, wifiPerms);
                } else {
                    getWifiSSID(args, callbackContext);
                }
	    	}else if(action.equals("smartConfig")){
	    		smartConfig(args, callbackContext);
	    	}else if(action.equals("cancelSmartConfig")){
	    		mEsptouchTask.interrupt();
	    		callbackContext.success(0);
	    	}else if(action.equals("sendBroadCast")){
	    		sendBroadCast(args, callbackContext);
	    	}else if(action.equals("receiveBroadCast")){
	    		receiveBroadCast(args, callbackContext);
	    	}else if(action.equals(GET_PREFERRED_LANGUAGE)){
	    		callbackContext.success(Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
	    	}else if(action.equals("showMsg")){
	    		showMsg(args.getString(0));
	    	}else if(action.equals("hideMsg")){
	    		hideMsg();
	    	}else if(action.equals(CONNECT_WIFI)) {
	    		connectWiFi(args, callbackContext);
	    	}else{
	    		callbackContext.error("no such method: " + action);
	    		return false;
	    	}
    	} catch (JSONException e) {
    		callbackContext.error("Parameters Error");
			e.printStackTrace();
			return false;
		}
    	return true;
    }
    /**
     * 获取App版本号，如10000
     */
    public void getVersionCode(JSONArray args, CallbackContext callbackContext){
    	try {
			PackageManager packageManager = this.cordova.getActivity().getPackageManager();
			callbackContext.success(String.valueOf(packageManager.getPackageInfo(this.cordova.getActivity().getPackageName(), 0).versionCode));
		} catch (NameNotFoundException e) {
			callbackContext.success("N/A");
		}
    }
    /**
     * 升级App
     */
    public void update(JSONArray args, CallbackContext callbackContext) throws JSONException {
    	if(true){
    		Activity activity = this.cordova.getActivity();
            String updateUrl = args.getString(0);
            this.updateManager = new UpdateManager(args, callbackContext, activity, updateUrl);
        	this.updateManager.update(this.webView);
        	callbackContext.success(0);
    	}else{//使用系统下载工具下载
    		Activity context = this.cordova.getActivity();
            String downloadUrl = args.getString(0);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
    		request.setAllowedOverRoaming(false);//漫游网络是否可以下载
    		//设置文件类型，可以在下载结束后自动打开该文件
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(downloadUrl));
            request.setMimeType(mimeString);
            String downFileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
    		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downFileName);
    		downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    		mTaskId = downloadManager.enqueue(request);
    		context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            callbackContext.success(0);
    	}
    }
    /**
     * 获取手机WiFI的SSID
     */
    public void getWifiSSID(JSONArray args, CallbackContext callbackContext){
    	JSONObject result = new JSONObject();
    	try {
    		WifiManager mWifiManager = (WifiManager) this.cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
    		WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
    		String wifiSsid = mWifiInfo.getSSID();
    		if(null != wifiSsid && wifiSsid.startsWith("\"")){
    			wifiSsid = wifiSsid.substring(1, wifiSsid.length() - 1);
    		}
    		result.put("ssid", wifiSsid);
    		result.put("bssid", mWifiInfo.getBSSID());
    		callbackContext.success(result);
		} catch (Exception e) {
			callbackContext.success(result);
		}
    }
    /**
     * 使用smartConfig发送WiFi配置
     */
    public void smartConfig(final JSONArray args, final CallbackContext callbackContext){
    	try {
    		final String apSsid = new String(args.getString(0).getBytes(), "ISO-8859-1");
    		final String apBssid = args.getString(1);
			final String apPassword = args.getString(2);
			final int taskResultCount = args.getInt(3);
			final Object mLock = new Object();
    		cordova.getThreadPool().execute(
				new Runnable(){
					public void run(){
						synchronized (mLock) {
	                        mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, cordova.getActivity());
	                        mEsptouchTask.setEsptouchListener(myListener);
						}
						List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
						JSONObject result = new JSONObject();
						IEsptouchResult succResult = null;
						for(int i=0; i<resultList.size(); i++){
							succResult = resultList.get(i);
							try {
								if (!succResult.isCancelled()){
									if(succResult.isSuc()){
										result.put("retCode", 0);
										result.put("bssid", succResult.getBssid());
										result.put("ip", succResult.getInetAddress().getHostAddress());
										break;
									}else{
										result.put("retCode", -1);
									}
								}else{
									result.put("retCode", 1);
								}
							} catch (JSONException e) {
								e.printStackTrace();
								callbackContext.error("JSON Error");
							}
						}
						callbackContext.success(result);
					}
				}
    		);
		} catch (Exception e) {
			callbackContext.error("Parameters Error");
		}
    }
    
	//检查下载状态
    private void checkDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(mTaskId);//筛选下载任务，传入任务ID，可变参数
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                case DownloadManager.STATUS_PENDING:
                case DownloadManager.STATUS_RUNNING:
                case DownloadManager.STATUS_FAILED:
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    //下载完成安装APK
					String downloadPath = null;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						String fileUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
						if (fileUri != null) {
							downloadPath = Uri.parse(fileUri).getPath();
						}
					}else{
						downloadPath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
					}
                    installAPK(downloadPath);
                    break;
            }
        }
    }
    
	//下载到本地后执行安装
    protected void installAPK(String fileUrl) {
        if (fileUrl == null) return;
		File apkFile = new File(fileUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			Uri contentUri = FileProvider.getUriForFile(cordova.getActivity(), cordova.getActivity().getApplicationInfo().packageName + ".provider", apkFile);
			intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
		}else{
			intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
		}
        cordova.getActivity().startActivity(intent);
    }
    /**
     * 发送UDP广播
     */
    public void sendBroadCast(final JSONArray args, final CallbackContext callbackContext){
    	try {
    		final String sendIp;
    		if(args.isNull(0)){
    			String localIp = getIPAddress();
    			sendIp = localIp.substring(0, localIp.lastIndexOf('.')) + ".255";
    		}else{
    			sendIp = args.getString(0);
    		}
    		final String sendMess = args.getString(1);
    		DatagramSocket clientSocket = new DatagramSocket();
			byte[] data = sendMess.getBytes();
			DatagramPacket dataPacket = new DatagramPacket(data, data.length, InetAddress.getByName(sendIp), 1025);
			clientSocket.send(dataPacket);
			clientSocket.close();
			clientSocket = null;
			callbackContext.success(0);
		} catch (Exception e){
			e.printStackTrace();
			callbackContext.success(-1);
		}
    }
    /**
     * 接收UDP广播
     */
    public void receiveBroadCast(final JSONArray args, final CallbackContext callbackContext){
    	try {
    		new ReceiveUdpBroadCast(callbackContext).start();
		} catch (Exception e){
			e.printStackTrace();
		}
    }
    
    /**
     * 接收Udp广播的线程.
     */
    private class ReceiveUdpBroadCast extends Thread{
    	CallbackContext callbackContext;
    	
    	public ReceiveUdpBroadCast(CallbackContext callbackContext){
    		 this.callbackContext = callbackContext;
    	}
    	
    	@Override
        public void run(){
    		try {
    			byte[] data = new byte[1024];
    			DatagramSocket dataSocket = new DatagramSocket(null);
    			dataSocket.setReuseAddress(true);
    			dataSocket.bind(new InetSocketAddress(1034));
    			dataSocket.setSoTimeout(20*1000);
				DatagramPacket dataPacket = new DatagramPacket(data, data.length);
				while(!Thread.currentThread().isInterrupted()){
					dataSocket.receive(dataPacket);
					String dataStr = new String(dataPacket.getData(), 0, dataPacket.getLength());
					//System.out.println(dataStr);
					if(null != dataStr && dataStr.length() >= 1){
						callbackContext.success(dataStr);
						dataSocket.close();
						dataSocket = null;
						interrupt();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				callbackContext.success("{}");
			}
    	}
    }
    
    public void showMsg(final String msg){
    	cordova.getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run(){
            	if(toastLayout == null){
            		Activity mainActivity = cordova.getActivity();
            		toastView = new TextView(mainActivity);
            		toastView.setTextSize(18);
            		toastView.setTextColor(Color.WHITE);
            		toastView.setVisibility(View.VISIBLE);
            		toastView.setText(msg);
                	
                	toastLayout = new RelativeLayout(mainActivity);
                	
                	RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                	layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                	toastLayout.addView(toastView, layoutParams);

                	Point deviceSize = new Point();
                	mainActivity.getWindowManager().getDefaultDisplay().getSize(deviceSize);
                	mainActivity.addContentView(toastLayout, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, deviceSize.x));
            	}else{
            		toastView.setText(msg);
            		toastLayout.setVisibility(View.VISIBLE);
            	}
            }
    	});
    }
    
    public void hideMsg(){
    	if(toastLayout != null && toastLayout.getVisibility() != View.GONE){
    		cordova.getActivity().runOnUiThread(new Runnable(){
                @Override
                public void run(){
                	toastLayout.setVisibility(View.GONE);
                }
        	});
    	}
    }

    /**
     * 连接指定WiFi
     */
    public void connectWiFi(final JSONArray args, final CallbackContext callbackContext) {
    	try {
    		final String ssid = args.getString(0);
    		final String password = args.getString(1);
            final WifiAdmin wifiAdmin = new WifiAdmin(cordova.getActivity());
            //wifiAdmin.openWifi();
            wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, password, 1));
            cordova.getThreadPool().execute(new Runnable(){
    			public void run(){
    				for(int i=0; i<5; i++) {
    					try {
        					Thread.sleep(1500);
    						if (wifiAdmin.getSSID(true).equals("\""+ssid+"\"")) {
    							callbackContext.success(0);
    							break;
    	                    }else if(i <= 3){
    	                    	wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, password, 1));
    	                    }else {
    	                    	callbackContext.error("Timeout Error");
    	                    }
    					} catch (InterruptedException e) {
    						e.printStackTrace();
    						callbackContext.error("AddNetwork Error");
    						break;
    					}
    				}
    			}
    		});
        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error("Parameters Error");
        }
    }
    
    private String getIPAddress(){
		WifiManager wifiManager = (WifiManager) cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		String ipString = String.format(
			"%d.%d.%d.%d",
			(ip & 0xff),
			(ip >> 8 & 0xff),
			(ip >> 16 & 0xff),
			(ip >> 24 & 0xff)
		);
		return ipString;
	}
    /**
     * check application's permissions
     */
	@Override
	public boolean hasPermisssion(String [] permissions) {
		for (String p : permissions) {
			if (!PermissionHelper.hasPermission(this, p)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * processes the result of permission request
	 *
	 * @param requestCode
	 *            The code to get request action
	 * @param permissions
	 *            The collection of permissions
	 * @param grantResults
	 *            The result of grant
	 */
	@Override
	public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
			throws JSONException {
		PluginResult result;
		for (int r : grantResults) {
			if (r == PackageManager.PERMISSION_DENIED) {
				Log.d(TAG, "Permission Denied!");
				this.callbackContext.success(1);
				return;
			}
		}
		switch (requestCode) {
			case 0:
				update(this.requestArgs, this.callbackContext);
			case 0:
                getWifiSSID(this.requestArgs, this.callbackContext);
			break;
		}
	}
}
