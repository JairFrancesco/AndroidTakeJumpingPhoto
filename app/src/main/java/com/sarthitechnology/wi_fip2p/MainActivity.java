package com.sarthitechnology.wi_fip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import java.lang.Math;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    Button btnOnOff, btnDiscover, btnSend, btnCamera;
    ListView listView;
    TextView read_msg_box, connectionStatus,sensorData, sensorDataGravity, arelative, acceleration_magnitude, acceleration_vertical;
    TextView time_highest;
    EditText writeMsg;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    private double x, y, z;
    private double threshold =0;

    List<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ=1;

    Server serverClass;
    Client clientClass;
    SendReceive sendReceive;

    //Usando el sensor de gravedad
    private SensorManager sensorManager;
    private Sensor sensor;
    private long lastUpdate;


    //variables para calcular t_highest
    float[][] a_measured;
    float[][] a_relative;
    float[][] a_gravity; //Vector gravedad: Indica la dirección y magnitud de la gravedad, obtenido del sensor de gravedad
    static final float g = 9.81f; //constant;
    float[][] a_vertical;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case MESSAGE_READ:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);

                    if (tempMsg == "picture") { //take picture
                        Toast.makeText(getApplicationContext(),"Foto tomada",Toast.LENGTH_SHORT).show();
                    } else {
                        read_msg_box.setText(tempMsg);
                    }

                    break;
            }
            return true;
        }
    });

    private void exqListener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wifiManager.isWifiEnabled())
                {
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("ON");
                }else {
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Starting Failed");
                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device=deviceArray[i];
                WifiP2pConfig config=new WifiP2pConfig();
                config.deviceAddress=device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Connected to "+device.deviceName,Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        /*
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg=writeMsg.getText().toString();
                sendReceive.write(msg.getBytes());
            }
        });
        */

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = writeMsg.getText().toString();
                byte[] bytes =msg.getBytes();
                btnSend.setVisibility(View.INVISIBLE);
                String userType = connectionStatus.getText().toString();
                if(userType.equals("Host")) {
                    serverClass.writeData(bytes);
                } else {
                    clientClass.writeData(bytes);
                }
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                //intent.putExtra(EXTRA_MESSAGE, message);
                startActivity(intent);
            }
        });

        //sensor de gravedad
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        lastUpdate = System.currentTimeMillis();
    }

    /*SENSOR LISTENER*/
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            getGravitySensor(event);
        }

    }

    private void getGravitySensor(SensorEvent event){
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];

        //
        a_gravity = new float[1][3];
        a_gravity[0][0] = (float)x;
        a_gravity[0][1] = (float)y;
        a_gravity[0][2] = (float)z;
        //

        x = Math.round(x * 100.0)/100.0;
        y = Math.round(y * 100.0)/100.0;
        z = Math.round(z * 100.0)/100.0;

        /*
        X.setText(x + "");
        Y.setText(y + "");
        Z.setText(z + "");
        */
        /*
        if(threshold - z < 0.05 && threshold - z > -0.05){
            Toast.makeText(this, "Flat Surface", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Not a Flat Surface", Toast.LENGTH_SHORT).show();
        }
        */

        String sensorxyz = "X:" + String.valueOf(x) + ",Y:" + String.valueOf(y) + ",Z:" + String.valueOf(z);
        sensorDataGravity.setText(sensorxyz);
    }

    //Computing the time of the highest point
    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        //
        a_measured = new float[1][3];
        a_measured[0][0] = x;
        a_measured[0][1] = y;
        a_measured[0][2] = z;
        //

        double accelerationmagnitude = Math.sqrt(x * x + y * y + z * z);
        acceleration_magnitude.setText("acceleration_magnitude:" + String.valueOf(accelerationmagnitude));

        float accelationSquareRoot = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        long actualTime = event.timestamp;
        /*
        if (accelationSquareRoot >= 2) //
        {
            if (actualTime - lastUpdate < 200) {
                return;
            }
            lastUpdate = actualTime;
            Toast.makeText(this, "Device was shuffed", Toast.LENGTH_SHORT).show();
        }

         */
        String sensorxyz = "X:" + String.valueOf(x) + ",Y:" + String.valueOf(y) + ",Z:" + String.valueOf(z);
        sensorData.setText(sensorxyz);



        //
        /*
        * When the sensor is at rest, the direction of a measured is towards the center of the Earth and its magnitude is ||ameasured|| = g = 9.81m/s 2
        * */
        //
        if (accelerationmagnitude > 10 && a_gravity!=null) { //10 > 9.81, sensor en movimiento, not rest
            a_relative = new float[1][3];
            a_relative[0][0] = a_measured[0][0] - a_gravity[0][0];
            a_relative[0][1] = a_measured[0][1] - a_gravity[0][1];
            a_relative[0][2] = a_measured[0][2] - a_gravity[0][2];

            //Calculate a_vertical
            float numerator = a_relative[0][0]*a_gravity[0][0] + a_relative[0][1]*a_gravity[0][1] + a_relative[0][2]*a_gravity[0][2];

            /*
            * On an n-dimensional Euclidean space Rn, the intuitive notion of length of the vector x = (x1, x2, ..., xn) is captured by the formula
            ||x||_2 = sqrt(x1**2 + x2**2 ++ xn**2)
            */
            a_vertical = new float[1][3];
            float denominator = a_gravity[0][0]*a_gravity[0][0] + a_gravity[0][1]*a_gravity[0][1] + a_gravity[0][2] * a_gravity[0][2];
            a_vertical[0][0] = (-1)*(numerator/denominator)*a_gravity[0][0];
            a_vertical[0][1] = (-1)*(numerator/denominator)*a_gravity[0][1];
            a_vertical[0][2] = (-1)*(numerator/denominator)*a_gravity[0][2];

            double v0 = Math.sqrt(Math.pow(a_vertical[0][0], 2) +Math.pow(a_vertical[0][1], 2)  + Math.pow(a_vertical[0][2], 2) );// acceleration_vertical_magnitude
            acceleration_vertical.setText("vertical velocity (v0):" + String.valueOf(v0));

            float t_highest = (float)v0/g;
            time_highest.setText("t_highest" + String.valueOf(t_highest));
            //Time of the highest point

            //send message to other device to take the picture

            String msg = "picture";
            byte[] bytes =msg.getBytes();
            btnSend.setVisibility(View.INVISIBLE);
            String userType = connectionStatus.getText().toString();
            if(userType.equals("Host")) {
                //serverClass.writeData(bytes);
                //return;
            } else {
                clientClass.writeData(bytes);
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*END SENSOR LISTENER*/

    private void initialWork() {
        btnOnOff=(Button) findViewById(R.id.onOff);
        btnDiscover=(Button) findViewById(R.id.discover);
        btnSend=(Button) findViewById(R.id.sendButton);
        btnCamera=(Button) findViewById(R.id.camera);

        listView=(ListView) findViewById(R.id.peerListView);
        read_msg_box=(TextView) findViewById(R.id.readMsg);
        connectionStatus=(TextView) findViewById(R.id.connectionStatus);
        sensorData = (TextView) findViewById(R.id.sensorData);
        sensorDataGravity = (TextView) findViewById(R.id.sensorDataGravity);

        //arelative = (TextView) findViewById(R.id.arelative);
        acceleration_magnitude = (TextView) findViewById(R.id.acceleration_magnitude);
        acceleration_vertical = (TextView) findViewById(R.id.acceleration_vertical);
        time_highest = (TextView) findViewById(R.id.time_highest);

        writeMsg=(EditText) findViewById(R.id.writeMsg);

        connectionStatus = (TextView)findViewById(R.id.connectionStatus);

        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager= (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel=mManager.initialize(this,getMainLooper(),null);

        mReceiver=new WiFiDirectBroadcastReceiver(mManager, mChannel,this);

        mIntentFilter=new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    WifiP2pManager.PeerListListener peerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if(!peerList.getDeviceList().equals(peers))
        {
            peers.clear();
            peers.addAll(peerList.getDeviceList());

            deviceNameArray=new String[peerList.getDeviceList().size()];
            deviceArray=new WifiP2pDevice[peerList.getDeviceList().size()];
            int index=0;

            for(WifiP2pDevice device : peerList.getDeviceList())
            {
                deviceNameArray[index]=device.deviceName;
                deviceArray[index]=device;
                index++;
            }

            ArrayAdapter<String> adapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
            listView.setAdapter(adapter);
        }

        if(peers.size()==0)
        {
            Toast.makeText(getApplicationContext(),"Ningún dispositivo encontrado",Toast.LENGTH_SHORT).show();
            return;
        }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener=new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        final InetAddress groupOwnerAddress=wifiP2pInfo.groupOwnerAddress;

        if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner)
        {
            connectionStatus.setText("Host");
            serverClass=new Server();
            serverClass.execute();
        }else if(wifiP2pInfo.groupFormed)
        {
            connectionStatus.setText("Client");
            clientClass=new Client(groupOwnerAddress);
            clientClass.execute();
        }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        sensorManager.unregisterListener(this);
    }

    public class Server extends AsyncTask<String, Integer, Boolean> {
        Socket socket;
        ServerSocket serverSocket;
        InputStream inputStream;
        OutputStream outputStream;
        @Override
        protected Boolean doInBackground(String... strings) {
            boolean result = true;
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
            } catch (IOException e) {
                result = false;
                e.printStackTrace();
            }
            return result;
        }

        public void writeData(final byte[] bytes) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            btnSend.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //listener
                new Thread(new Runnable(){
                    public void run() {
                        byte[] buffer = new byte[1024];
                        int x;
                        while (socket!=null) {
                            try {
                                x = inputStream.read(buffer);
                                if(x>0) {
                                    handler.obtainMessage(MESSAGE_READ,x,-1,buffer).sendToTarget();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
                btnSend.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(),"could not create sockets",Toast.LENGTH_SHORT).show();
                //restart socket assignment process
            }
        }
    }

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt)
        {
            socket=skt;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer=new byte[1024];
            int bytes;

            while (socket!=null)
            {
                try {
                    bytes=inputStream.read(buffer);
                    if(bytes>0)
                    {
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public class Client extends AsyncTask<String, Integer, Boolean> {
        Socket socket;
        String hostAdd;
        InputStream inputStream;
        OutputStream outputStream;

        public Client(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            boolean result = false;
            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888), 5000);
                result = true;
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                return result;
            }
        }

        public void writeData(final byte[] bytes) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            btnSend.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new Thread(new Runnable(){
                    public void run() {
                        byte[] buffer = new byte[1024];
                        int x;
                        while (socket!=null) {
                            try {
                                x = inputStream.read(buffer);
                                if(x>0) {
                                    handler.obtainMessage(MESSAGE_READ,x,-1,buffer).sendToTarget();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
                btnSend.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(),"could not create sockets",Toast.LENGTH_SHORT).show();
                //restart socket assignment process
            }
        }
    }

}

