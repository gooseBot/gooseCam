package com.time2go.goosecam;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CamActivity extends Activity {

    private static final int UDP_SERVER_PORT = 2004;
    private static final int MAX_UDP_DATAGRAM_LEN = 1500;
    private TextView textMessage;
    private RunServerInThread runServer = null;
    private Handler mHandler = new Handler();
    public final static String DEBUG_TAG = "CamActivity";

    private Camera mCamera;
    private CameraPreview mPreview;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static String mSessionID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);

        textMessage = (TextView) findViewById(R.id.textMessage);
        runServer = new RunServerInThread();
        runServer.start();
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        if (runServer != null) {
            runServer.stopMe();
        }
        releaseCamera();              // release the camera immediately on pause event
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Create an instance of Camera
        if (mCamera==null) {
            mCamera = getCameraInstance();
            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();  // Always call the superclass method first
        if (runServer != null) {
            runServer.startMe();
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private class RunServerInThread extends Thread{
        volatile boolean keepRunning = true;

        @Override
        public void run() {
            //final String message;
            byte[] lmessage = new byte[MAX_UDP_DATAGRAM_LEN];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);
            DatagramSocket socket = null;
            while(keepRunning){
                try {
                    socket = new DatagramSocket(UDP_SERVER_PORT);
                    socket.receive(packet);
                    final String message = new String(lmessage, 0, packet.getLength());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // This gets executed on the UI thread so it can safely modify Views
                            textMessage.setText(message);
                            capturePicture(message);
                        }
                    });
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) {
                        socket.close();
                    }
                }
            }
        }

        public void stopMe()
        {
            keepRunning = false;
        }

        public void startMe()
        {
            keepRunning = true;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Camera.Parameters params = c.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            //looks like highest resolution is the first element.
            params.setPictureSize(sizes.get(0).width, sizes.get(0).height);
            c.setParameters(params);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(DEBUG_TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d(DEBUG_TAG, "Wrote file: " + pictureFile.toString());
            } catch (FileNotFoundException e) {
                Log.d(DEBUG_TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(DEBUG_TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    public void capturePicture(String sessionUID) {
        mSessionID=sessionUID.replaceAll("[^\\d.]", "");
        mCamera.takePicture(null, null, mPicture);
        mCamera.startPreview();
    }

    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+  mSessionID + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

}
