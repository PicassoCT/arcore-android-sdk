package com.google.ar.core.examples.java.common.tcpClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.common.rendering.SpringOverlayRenderer;
import com.google.ar.core.examples.java.helloar.SpringARActivity;

public class Server {
    public static final int SERVERPORT = 8090 ;
    private ServerSocket serverSocket;
    IPackageRecivedCallback packageRecipient;

    Thread serverThread = null;
    static String sendCFGHeader ="SPRINGARREC;CFG=";
    static  String sendCAMHeader ="SPRINGARCAM;DATA=";
    static String recieveDataHeader = "SPRINGARSND;DATA=";
    static String recieveResetHeader = "SPRINGAR;RESET;";
    static String seperator = ";";
    public TwinBuffer Buffer;

    private Pose groundAnchorPose;
    private Pose cameraPose;


    int messageCounter; //Zählt die Anzahl der erhaltenen Messages
    public boolean stillConnected;

    private void resetAllMessages(String Message) {
        if (Message.indexOf(recieveResetHeader) != 0)
         messageCounter = 0;
    }

    private String formConfigurationMessage() {
        String message = "";

        message = sendCFGHeader +
                Build.MODEL + seperator +//devicename
                Resources.getSystem().getDisplayMetrics().widthPixels + seperator +// screen width
                Resources.getSystem().getDisplayMetrics().heightPixels + seperator+// screen heigth
                50 + seperator;// divider
        return message;
    }

    private String formCamMatriceMessage(Pose camPose, Pose anchorPose) {
        String message = sendCAMHeader;
        float  mat4_4[] = new float[16];
        camPose.toMatrix(mat4_4, 0);

        for (int i= 0;i < 16;i++) {
            message +=  (mat4_4[i] + seperator);
        }

    return message;
    }

    public void Server(Bundle savedInstanceState, IPackageRecivedCallback packageRecipient) {
        this.packageRecipient = packageRecipient;

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    public void halt() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateCam_GroundAnchor(Pose camera, Pose groundAnchor) {
        cameraPose = camera;
        groundAnchorPose = groundAnchor;
    }

    class ServerThread implements Runnable {
        ServerThread() {

        }

        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket, packageRecipient);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Recives Data from the socket
    class CommunicationThread implements Runnable {

        private Socket clientSocket;
        private BufferedWriter output;
        private BufferedReader input;
        IPackageRecivedCallback packageRecipient;

        public CommunicationThread( Socket clientSocket, IPackageRecivedCallback packageRecipient) {
            this.packageRecipient = packageRecipient;
            this.clientSocket = clientSocket;
            try{
                //TODO Quick & Dirty - needs Cleanup - Cameramatrice oder Phoneconfiguration an den Spielserver senden
                if (messageCounter  == 0)
                     this.output = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream(),
                                                                            formConfigurationMessage()));
                else {
                    this.output = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream(),
                            formCamMatriceMessage( cameraPose, groundAnchorPose)
                            ));
                }
            }  catch (IOException e) {
                e.printStackTrace();
            }

            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Empfangen
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {

                    String read = input.readLine();
                    //TODO Remove Debug String
                    Log.d(this.getClass().getSimpleName(), read);

                    //Setzt die Verbindung zurück wenn der Host das anfordert
                    resetAllMessages(read);

                    if (read.contains(recieveDataHeader)) {
                        read.replace(recieveDataHeader,"");
                        Bitmap refToWriteBuffer = Buffer.getWriteBuffer();
                        //noinspection UnusedAssignment
                        refToWriteBuffer = new BitmapFactory().decodeStream( this.clientSocket.getInputStream());
                        Buffer.switchBuffer();
                        packageRecipient.callback( Buffer.getDrawBuffer());
                        }

                    }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            }

    }
}

