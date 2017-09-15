package com.example.llln.autowifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by llln on 2017/7/31.
 * 此类为自动连接指定WIFI,主要用于相关测试.
 * 目测只测试过无密码的情况.
 * 提供其他的WIFI信息单并未测试.
 * 目前没有对6.0进行适配.
 * 1.添加相应权限:
 * <p>
 * <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" ></uses-permission>
 * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" ></uses-permission>
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" ></uses-permission>
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" ></uses-permission>
 * <p>
 * 2.使用方式:
 * (1)初始化WifiAdmin
 * WifiAdmin.getInstance() //获取实例
 * .Init(this)         //初始化
 * .setConnectWifiName("TP") //需要连接的WIFI名称
 * .addCallBack(this) //设置回调接口监听,没有做非空判断
 * .setTimeOutCount(30)    //设置超时次数
 * .setScanTime(1000);      //设置查询间隔,默认为1s.
 * <p>
 * (2)开始连接: WifiAdmin.getInstance().openWifi().startScan().startAutoWIFI();
 * <p>
 * (3)关闭连接: WifiAdmin.getInstance().disconnectWifi();
 * <p>
 *
 */

public class WifiAdmin {
    // 定义WifiManager对象
    private WifiManager mWifiManager;
    // 定义WifiInfo对象
    private WifiInfo mWifiInfo;
    // 扫描出的网络连接列表
    private List<ScanResult> mWifiList;
    // 网络连接列表
    private List<WifiConfiguration> mWifiConfiguration;
    // 定义一个WifiLock
    private WifiManager.WifiLock mWifiLock;
    //网络连接状态对象
    private ConnectivityManager connManager;
    //网络信息对象
    private NetworkInfo networkInfo;
    //Context
    private Context mContext;
    //需要连接的WIFI,SSID 可以是部分也可以是全部.
    private String mSSIDPart;
    //获取到WIFI列表的SSID全称
    private String mSSIDFullName;
    //WIFI连接成功的Callback
    private WifiConnectListener mWifiConnectListener;

    //定时器 查询WIFI连接状态
    private Timer mTimer;
    //是否是第一次连接
    private boolean isFirst = false;
    //控制是否继续查询
    private boolean timerTag = true;
    //是否已经开始建立连接
    private boolean isStartConnect = false;
    //查询次数,可以以此来设置超时时间.
    private int timerRunCount = 0;
    //查询超时次数
    private int timeOutCount;
    //扫描间隔时间
    private int scanTime = 1000;
    //连接成功
    private boolean isConnectSuccess;
    //wcgId,netId.
    private int wcgId;

    private static volatile WifiAdmin Instance = null;

    //提供一个空的构造器
    public WifiAdmin() {
    }

    public static WifiAdmin getInstance() {
        if (Instance == null) {
            synchronized (WifiAdmin.class) {
                if (Instance == null) {
                    Instance = new WifiAdmin();
                }
            }
        }
        return Instance;
    }


    /**
     * @param context 上下文
     **/
    WifiAdmin Init(Context context) {
        this.mContext = context;
        if (mWifiManager == null)
            mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象
        mWifiInfo = mWifiManager.getConnectionInfo();
        return Instance;
    }

    /**
     * @param wifiConnectListener wifi连接的监听
     **/
    WifiAdmin addCallBack(WifiConnectListener wifiConnectListener) {
        this.mWifiConnectListener = wifiConnectListener;
        return Instance;
    }

    WifiAdmin setConnectWifiName(String SSID) {
        this.mSSIDPart = SSID;
        return Instance;
    }

    // 打开WIFI
    WifiAdmin openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        return Instance;
    }

    // 关闭WIFI
    public WifiAdmin closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            Log.d("tag2", "WIFI 已经关闭");
            mWifiManager.setWifiEnabled(false);
        }
        return Instance;
    }

    // 检查当前WIFI状态
    public int checkState() {
        return mWifiManager.getWifiState();
    }

    // 锁定WifiLock
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // 解锁WifiLock
    public void releaseWifiLock() {
        // 判断时候锁定
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // 创建一个WifiLock
    public void creatWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("Test");
    }

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    // 指定配置好的网络进行连接
    public void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId, true);
    }

    private boolean checkSSID() {
        //初始化wifi连接管理器
        if (connManager == null)
            connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        //get current SSID
        String ssid = mWifiManager.getConnectionInfo().getSSID().replace("\"", "");
        return networkInfo.isConnected() && ssid.contains(mSSIDPart);
    }


    //开始扫描
    WifiAdmin startScan() {
        mWifiManager.startScan();
        // 得到扫描结果
        mWifiList = mWifiManager.getScanResults();
        // 得到配置好的网络连接
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
        return Instance;
    }

    WifiAdmin setTimeOutCount(int timeOutCount) {
        this.timeOutCount = timeOutCount;
        return Instance;
    }

    WifiAdmin setScanTime(int scanTime) {
        this.scanTime = scanTime;
        return Instance;
    }


    /**
     *
     **/
    void startAutoWIFI() {

        if (mSSIDPart == null)
            throw new NullPointerException("Did you remember to add the WIFI name?");
        if (getConnInfo()) deleteCurrentWIFI(wcgId);
        if (mTimer == null) mTimer = new Timer();
        if (!isFirst) mTimer.schedule(new CheckWifiTime(), 1000, scanTime); //间隔1s再启动
        mWifiConnectListener.startConnect();
//        Log.d("tag2", "开始连接扫描  -- callback ,---Thread---" + Thread.currentThread().getId());
        //timer 只启动一次.
        isFirst = true;
        //初始化状态
        timerTag = true;
        isStartConnect = false;
        timerRunCount = 0;
    }


    // 得到网络列表
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    /**
     * @return 返回指定SSID的全部名称
     **/
    private String lookUpScan() {
//        StringBuilder stringBuilder = new StringBuilder();
//        for (int i = 0; i < mWifiList.size(); i++) {
//            stringBuilder.append("Index_" + new Integer(i + 1).toString() + ":");
//            // 将ScanResult信息转换成一个字符串包
//            // 其中把包括：BSSID、SSID、capabilities、frequency、level
//            stringBuilder.append((mWifiList.get(i)).toString());
//            stringBuilder.append("/n");
//        }
//        return stringBuilder;
        for (int i = 0; i < mWifiList.size(); i++) {
            if (mWifiList.get(i).SSID.contains(mSSIDPart)) {
                mSSIDFullName = mWifiList.get(i).SSID;
                return mSSIDFullName;
            }
        }
        return "";
    }

    public boolean getConnInfo() {
        return isConnectSuccess;
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

    public int getWcgId() {
        return wcgId;
    }

    // 添加一个网络并连接
    private void addNetwork(WifiConfiguration wcg) {
        wcgId = mWifiManager.addNetwork(wcg);
        isConnectSuccess = mWifiManager.enableNetwork(wcgId, true);
        Log.d("tag2", " netID :" + wcgId + "  " + isConnectSuccess);
    }

    // 断开网络
    public void disconnectWifi() {
        if (getConnInfo()) deleteCurrentWIFI(wcgId);
        closeWifi(); //如果不关闭WIFI的话有较长时间的延迟,需重启WIFI.
        mWifiConnectListener.wifiIsDisConnected();
    }

    //不保存当前连接过的WIFI
    public void deleteCurrentWIFI(int netId) {
        mWifiManager.removeNetwork(netId);
        mWifiManager.saveConfiguration();
    }


    void Destroy() {
        if (null != mTimer) {
            mTimer.cancel();
            mTimer = null;
        }
    }


    private class CheckWifiTime extends TimerTask {
        @Override
        public void run() {
            if (!timerTag) {
                //此时表明已经连接成功,此处可以监听WIFI状态.
                if (!checkSSID()) {
                    closeWifi(); //如果不关闭WIFI的话有较长时间的延迟,需重启WIFI.
                    mWifiConnectListener.wifiIsDisConnected();
                }
                Log.d("tag2", "WIFI State: " + checkSSID());
                return;
            }
            //判断扫描的列表当中是否有我们想要连接的WIFI
            if (!"".equals(lookUpScan())) {
                if (!isStartConnect) {
                    mWifiConnectListener.onConnect(mSSIDFullName);
                    if (mSSIDFullName != null) addNetwork(CreateWifiInfo(mSSIDFullName, "", 0));
//                    Log.d("tag2", "正在建立连接 --  " + mSSIDFullName + " ---Thread---" + Thread.currentThread().getId());
               
                }
                isStartConnect = true;
            } else {
                if (timerRunCount >= timeOutCount) {
                    mWifiConnectListener.timeOut();
//                    Log.d("tag2","查询超时");
                    return;
                }
                timerRunCount++;
                startScan();
                mWifiConnectListener.onScan(timerRunCount);
//                Log.d("tag2", "列表当中不存在我们想要的WIFI,再次扫描 " + timerRunCount + " 次");
            }
            if (checkSSID()) {
//                Log.d("tag2", "连接成功 -- " + mSSIDFullName + " ---Thread---" + Thread.currentThread().getId());
                mWifiConnectListener.connectSuccess(mSSIDFullName);
                timerTag = false;
            }
        }
    }

    public void onStop() {
        //不可见时不扫描
        timerTag = false;
    }


    //然后是一个实际应用方法，只验证过没有密码的情况：
    //分为三种情况：0和1没有密码2用wep加密3用wpa加密
    private WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.IsExits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == 0) {
            config.preSharedKey = null;//非加密wifi
//      config.preSharedKey = "\"wifi密码\"";//加密wifi
            config.hiddenSSID = true;
            config.status = WifiConfiguration.Status.ENABLED;
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);//WPA_PSK  NONE（非加密）
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        }

        if (Type == 1) //WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == 2) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == 3) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private WifiConfiguration IsExits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        if (existingConfigs != null)
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        return null;
    }

    interface WifiConnectListener {


        void wifiIsDisConnected();//WIFI连接超时或者已经断开连接.


        void timeOut();//超时

        /**
         * 正在扫描回调
         *
         * @param count 扫描次数
         **/
        void onScan(int count);

        //开始扫描
        void startConnect();

        /**
         * 正在尝试连接
         *
         * @param onConnectSSID 连接成功的wifi名称
         **/
        void onConnect(String onConnectSSID);

        /**
         * 已经连接成功
         *
         * @param connectSSID 连接成功的wifi名称
         **/
        void connectSuccess(String connectSSID);

    }

}
