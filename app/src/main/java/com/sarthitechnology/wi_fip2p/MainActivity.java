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
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;
import java.lang.Math;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /*CAMERA*/
    private Camera mCamera;
    private MainActivity.CameraPreview mPreview;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                Toast.makeText(getApplicationContext(),"Error creando el archivo de medios, verifique los permisos de almacenamiento",Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Toast.makeText(getApplicationContext(),"Foto tomada",Toast.LENGTH_SHORT).show();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
                Toast.makeText(getApplicationContext(),"Archivo no encontrado",Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
                Toast.makeText(getApplicationContext(),"Error accediendo al archivo",Toast.LENGTH_SHORT).show();
            }
        }
    };

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "P2P Jump Photo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    FrameLayout preview;
    LinearLayout peersdata;
    /* END CAMERA*/

    Button btnOnOff, btnDiscover, btnSend, btnCamera;
    ListView listView;
    TextView read_msg_box, connectionStatus,sensorData, sensorDataGravity, arelative, acceleration_magnitude, acceleration_vertical;
    TextView time_highest, take_picture_at;
    TextView max_v0, min_v0;
    TextView max_time_highest;
    EditText writeMsg;

    Button btnRestartpreview;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ=1;
    Server serverClass;
    Client clientClass;

    //Usando el sensor de gravedad
    private SensorManager sensorManager;
    private Sensor sensor;
    private long lastUpdate;
    double x,y,z;
    float maximumv0 = 9.8f, minimumv0 = 10;
    float maximum_thighest = 0.2f;

    //variables para calcular t_highest
    float[][] a_measured;
    float[][] a_relative;
    float[][] a_gravity; //Vector gravedad: Indica la dirección y magnitud de la gravedad, obtenido del sensor de gravedad
    static final float g = 9.81f; //constant;
    float[][] a_vertical;


    //times to update and send notification to camera
    long curTime, lasUpdate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*CAMERA*/

        try {
            // Create an instance of Camera
            mCamera = getCameraInstance();

            // Create our Preview view and set it as the content of our activity.
            mPreview = new MainActivity.CameraPreview(this, mCamera);
            preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            peersdata = (LinearLayout) findViewById(R.id.peersdata);

            // Add a listener to the Capture button
            /*
            Button captureButton = (Button) findViewById(R.id.button_capture);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // get an image from the camera
                            mCamera.takePicture(null, null, mPicture);
                        }
                    }
            );
            */

            mCamera.setDisplayOrientation(90);
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "No se pudo acceder a la camara, porfavor conceda los permisos", Toast.LENGTH_SHORT).show();
        }

        /*END CAMERA*/

        initialWork();
        exqListener();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);


    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
        switch (msg.what)
        {
            case MESSAGE_READ:
                byte[] readBuff= (byte[]) msg.obj;
                String tempMsg=new String(readBuff,0,msg.arg1);

                Log.d("MENSAJE", "MENSAJE WIFI DIRECT RECIBIDO");
                Log.d("MENSAJE", tempMsg);

                if (tempMsg.equals("capturar")){
                    try {
                        mCamera.takePicture(null, null, mPicture);
                        btnRestartpreview.setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(), "Foto guardada", Toast.LENGTH_SHORT).show();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error, no se pudo tomar la foto", Toast.LENGTH_SHORT).show();
                    }
                }

                //
                if (tempMsg.contains(":")) {

                    try {
                        String[] parts = tempMsg.split(":");
                        float time_to_take_picture = Float.valueOf(parts[1]);
                        read_msg_box.setText("La foto sera tomada en :"  + parts[1]);
                        Log.d("TAKE PICTURE ON:", String.valueOf((long)(time_to_take_picture*1000)));
                        Thread.sleep((long)(time_to_take_picture*1000));
                        mCamera.stopPreview();
                        btnRestartpreview.setVisibility(View.VISIBLE);
                        mCamera.takePicture(null, null, mPicture);
                        Toast.makeText(getApplicationContext(), "Foto guardada", Toast.LENGTH_SHORT).show();
                        read_msg_box.setText("Foto tomada");

                    } catch(Exception e) {
                        e.printStackTrace();
                        // Process exception
                    }

                } else {
                    read_msg_box.setText(tempMsg);
                }
                //

                read_msg_box.setText(tempMsg);


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
                preview.setVisibility(View.GONE);
                peersdata.setVisibility(View.VISIBLE);
                btnRestartpreview.setVisibility(View.GONE);
            }else {
                wifiManager.setWifiEnabled(true);
                btnOnOff.setText("OFF");

            }
            }
        });

        btnRestartpreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRestartpreview.setVisibility(View.GONE);
                mCamera.startPreview();
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    connectionStatus.setText("Buscando dispositivos cercanos");
                }
                @Override
                public void onFailure(int i) {
                    connectionStatus.setText("Ocurrio un error al iniciar el descubrimiento");
                }
            });
            }
        });

        //on item peer to connect to it
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device=deviceArray[i];
                WifiP2pConfig config=new WifiP2pConfig();
                config.deviceAddress=device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Connectado a "+device.deviceName,Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"No conectado",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


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
        a_gravity = new float[1][3];
        a_gravity[0][0] = (float)x;
        a_gravity[0][1] = (float)y;
        a_gravity[0][2] = (float)z;
        x = Math.round(x * 100.0)/100.0;
        y = Math.round(y * 100.0)/100.0;
        z = Math.round(z * 100.0)/100.0;

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

        a_measured = new float[1][3];
        a_measured[0][0] = x;
        a_measured[0][1] = y;
        a_measured[0][2] = z;

        double accelerationmagnitude = Math.sqrt(x * x + y * y + z * z);
        acceleration_magnitude.setText("acceleration_magnitude:" + String.valueOf(accelerationmagnitude));
        String sensorxyz = "X:" + String.valueOf(x) + ",Y:" + String.valueOf(y) + ",Z:" + String.valueOf(z);
        sensorData.setText(sensorxyz);


        //When the sensor is at rest, the direction of a measured is towards the center of the Earth and its magnitude is ||ameasured|| = g = 9.81m/s 2
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

            if (v0 > maximumv0) {
                maximumv0 = (float) v0;
                max_v0.setText("Max (v0):"+String.valueOf(maximumv0));
            }


            if (v0 < minimumv0) {
                minimumv0 = (float) v0;
                min_v0.setText("Min (v0):"+String.valueOf(minimumv0));
            }

            float t_highest = (float)v0/g;
            time_highest.setText("t_highest: " + String.valueOf(t_highest));
            //Time of the highest point

            if (t_highest > maximum_thighest) {
                maximum_thighest = t_highest;
                max_time_highest.setText("Max (t_highest):" + String.valueOf(maximum_thighest));
            }

            //send message to other device to take the picture
            String msg = "picture at:"+t_highest;
            //String msg = "capturar";
            byte[] bytes =msg.getBytes();
            String userType = connectionStatus.getText().toString();

            //only allow one update every 20ms
            curTime = System.currentTimeMillis();
            long diffTime = curTime - lastUpdate;
            if (t_highest > 1.4 && diffTime > 100) { //update every 50 ms
                lastUpdate = curTime;
                // The predicted time of the highest point t highest is transmitted to the remote camera device
                if(userType.equals("Client")) {
                    clientClass.writeData(bytes);
                    //serverClass.writeData(bytes);
                    //return;
                }
            }

            /*
            if (t_highest > 1.3) {
                // The predicted time of the highest point t highest is transmitted to the remote camera device
                if(userType.equals("Client")) {
                    clientClass.writeData(bytes);
                    //serverClass.writeData(bytes);
                    //return;
                }
            }*/

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

        btnRestartpreview = findViewById(R.id.restartpreview);

        listView=(ListView) findViewById(R.id.peerListView);
        read_msg_box=(TextView) findViewById(R.id.readMsg);
        connectionStatus=(TextView) findViewById(R.id.connectionStatus);
        sensorData = (TextView) findViewById(R.id.sensorData);
        sensorDataGravity = (TextView) findViewById(R.id.sensorDataGravity);
        max_v0 = findViewById(R.id.max_v0);
        min_v0 = findViewById(R.id.min_v0);
        max_time_highest = findViewById(R.id.max_time_highest);
        take_picture_at = findViewById(R.id.take_picture_at);

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

                preview.setVisibility(View.VISIBLE);
                peersdata.setVisibility(View.GONE);

            }else if(wifiP2pInfo.groupFormed)
            {
                connectionStatus.setText("Client");
                clientClass=new Client(groupOwnerAddress);
                clientClass.execute();



            } else {
                preview.setVisibility(View.GONE);
                peersdata.setVisibility(View.VISIBLE);
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
        releaseCamera();              // release the camera immediately on pause event
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
                Toast.makeText(getApplicationContext(),"No se pudieron crear sockets",Toast.LENGTH_SHORT).show();
                //restart socket assignment process
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
                    } catch (Exception e) {
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
                Toast.makeText(getApplicationContext(),"No se pudieron crear sockets",Toast.LENGTH_SHORT).show();
                //restart socket assignment process
            }
        }
    }

}

