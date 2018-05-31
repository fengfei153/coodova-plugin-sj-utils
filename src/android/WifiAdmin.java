package com.fhsjdz.cordova.utils;

import java.util.List;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

public class WifiAdmin {
	private WifiManager mWifiManager;
    // 定义WifiInfo对象    
    private WifiInfo mWifiInfo;
    // 扫描出的网络连接列表    
    private List<ScanResult> mWifiList;
    // 网络连接列表    
    private List<WifiConfiguration> mWifiConfiguration;
  
   
    // 构造器    
    public WifiAdmin(Context context) {
        // 取得WifiManager对象    
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象    
        mWifiInfo = mWifiManager.getConnectionInfo();
    }
    // 打开WIFI
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    // 关闭WIFI
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    // 检查当前WIFI状态
    public int checkState() {
        return mWifiManager.getWifiState();
    }

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    public void startScan() {
        mWifiManager.startScan();
        // 得到扫描结果
        mWifiList = mWifiManager.getScanResults();
        // 得到配置好的网络连接
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
    }

    // 得到网络列表
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    // 查看扫描结果
    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mWifiList.size(); i++) {
            stringBuilder.append("Index_" + new Integer(i + 1).toString() + ":");
            // 将ScanResult信息转换成一个字符串包
            // 其中把包括：BSSID、SSID、capabilities、frequency、level
            stringBuilder.append((mWifiList.get(i)).toString());
            stringBuilder.append("/n");
        }
        return stringBuilder;
    }

    // 得到SSID
    public String getSSID(boolean isRefresh){
    	if(isRefresh) {
    		mWifiInfo = mWifiManager.getConnectionInfo();
    	}
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getSSID();
    }

    // 得到MAC地址
    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    // 得到接入点的BSSID
    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    // 得到IP地址
    public int getIPAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    // 得到连接的ID
    public int getNetworkId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    // 得到WifiInfo的所有信息包
    public String getWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }

    // 断开指定ID的网络
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }
	// 添加一个网络并连接
    public void addNetwork(WifiConfiguration wcg) {
    	//mWifiManager.disconnect();
        boolean status =  mWifiManager.enableNetwork(wcg.networkId, true);
        //mWifiManager.saveConfiguration();
        mWifiManager.reconnect();
        System.out.println("net status: " + wcg.networkId + "," + status);
    }
    //然后是一个实际应用方法，只验证过没有密码的情况：
    public WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration tempConfig = this.isExsits(SSID);
        if(tempConfig != null) {
            //System.out.println("has the exsits SSID :" + SSID);
            //mWifiManager.removeNetwork(tempConfig.networkId);
            return tempConfig;
        }else {
        	WifiConfiguration config = new WifiConfiguration();
        	config.allowedKeyManagement.clear();
        	config.allowedAuthAlgorithms.clear();
        	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        	config.SSID = "\"" + SSID + "\"";
        	config.priority = 999999;
            //config.allowedGroupCiphers.clear();
            //config.allowedPairwiseCiphers.clear();
            //config.allowedProtocols.clear();
            mWifiManager.addNetwork(config);
        	return config;
        }

//        if(Type == 1) {//WIFICIPHER_NOPASS
//            //config.wepKeys[0] = "";
//            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//            //config.wepTxKeyIndex = 0;
//        }
//        if(Type == 2) { //WIFICIPHER_WEP
//            config.hiddenSSID = true;
//            config.wepKeys[0]= "\""+Password+"\"";
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
//            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//            config.wepTxKeyIndex = 0;
//        }
//        if(Type == 3) { //WIFICIPHER_WPA
//            config.preSharedKey = "\""+Password+"\"";
//            config.hiddenSSID = true;
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
//            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//            config.status = WifiConfiguration.Status.ENABLED;
//        }
//        return config;
    }

    private WifiConfiguration isExsits(String SSID){
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        WifiConfiguration exsitConfig = null;
        for (WifiConfiguration config : configs) {
        	if (config.SSID.equals("\""+SSID+"\"")) {
        		exsitConfig = config;
            }else if(getSSID(false).equals(config.SSID)){
            	mWifiManager.disableNetwork(config.networkId);
            	mWifiManager.disconnect();
            }
        }
        return exsitConfig;
    }
}
