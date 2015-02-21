package com.time2go.goosecam;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class CamActivity extends Activity {

    private static final int UDP_SERVER_PORT = 2004;
    private static final int MAX_UDP_DATAGRAM_LEN = 1500;
    private TextView textMessage;
    private RunServerInThread runServer = null;
    private Handler mHandler = new Handler();

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
    }

    @Override
    protected void onStart() {
        super.onStart();  // Always call the superclass method first
        if (runServer != null) {
            runServer.startMe();
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
                        }
                    });
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
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
}
