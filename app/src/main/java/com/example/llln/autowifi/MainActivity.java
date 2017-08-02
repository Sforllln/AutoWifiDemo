package com.example.llln.autowifi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements WifiAdmin.WifiConnectListener {

    Button mConnect_btn;
    Button mDisConnect_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnect_btn = (Button) findViewById(R.id.btn_connect);
        mDisConnect_btn = (Button) findViewById(R.id.btn_disConnect);

        WifiAdmin.getInstance() //获取实例
                .Init(this)         //初始化
                .setConnectWifiName("5c64") //需要连接的WIFI名称,可以是全部也可以是部分,例如:5c64
                .addCallBack(this) //设置回调接口监听,没有做非空判断
                .setTimeOutCount(30)    //设置超时次数
                .setScanTime(1000);      //设置查询间隔,默认为1s.

        mConnect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiAdmin.getInstance().openWifi().startScan().startAutoWIFI();
            }
        });

        mDisConnect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiAdmin.getInstance().disconnectWifi();
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        WifiAdmin.getInstance().onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //提示重新开始
    }

    /**
     * 回调请注意线程
     */
    @Override
    public void wifiIsDisConnected() {

        Log.d("tag2", "WIFI未连接" + Thread.currentThread().getId());

    }

    @Override
    public void timeOut() {
        //扫描超时,如果是android6.0以上系统 需要打开位置服务相关权限.
        Log.d("tag2", "扫描超时" + Thread.currentThread().getId());
    }

    @Override
    public void onScan(int count) {
        //正在扫描
        Log.d("tag2", "正在扫描" + Thread.currentThread().getId());
    }

    @Override
    public void startConnect() {
        //开始扫描
        Log.d("tag2", "开始扫描" + Thread.currentThread().getId());
    }

    @Override
    public void onConnect(String onConnectSSID) {
        //正在连接
        Log.d("tag2", "正在连接" + Thread.currentThread().getId());
    }

    @Override
    public void connectSuccess(String connectSSID) {
        //连接成功
        Log.d("tag2", "连接成功" + Thread.currentThread().getId());
    }
}
