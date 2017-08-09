# AutoWifiDemo
 * 此类为自动连接指定WIFI,主要用于相关测试.
 * 目测只测试过无密码的情况.
 * 提供其他的WIFI信息单并未测试.
 * 目前没有对6.0进行适配.
 * 1.添加相应权限:
 <p>
 **<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>
 **<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
 **<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 **<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
 * 2.使用方式:
 <p>
 * 1)初始化WifiAdmin
 * WifiAdmin.getInstance() //获取实例
  .init(this)         //初始化
  .addCallBack(this) //设置回调接口监听,没有做非空判断
  .setTimeOutCount(30)    //设置超时次数
  .setScanTime(1000);      //设置查询间隔,默认为1s.

 * 2)开始连接: WifiAdmin.getInstance().setConnectWifiName("FPV").openWifi() .startScan().startAutoWIFI();
 * 3)关闭连接 WifiAdmin.getInstance().disconnectWifi();

