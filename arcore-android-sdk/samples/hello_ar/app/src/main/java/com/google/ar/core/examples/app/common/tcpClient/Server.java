package com.google.ar.core.examples.app.common.tcpClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.ar.core.Pose;
import com.google.ar.core.examples.app.springar.SpringARActivity;

public class Server {
    public static final int SERVERPORT = 8090;
    private static final String TAG = Server.class.getSimpleName();
    private ServerSocket serverSocket;
    IPackageRecivedCallback packageRecipient;

    Thread serverThread = null;
    static String sendCFGHeader = "SPRINGARREC;CFG=";
    static String sendCAMHeader = "SPRINGARCAM;DATA=";
    static String recieveDataHeader = "SPRINGARSND;DATA=";
    static String recieveResetHeader = "SPRINGAR;RESET;";
    static String seperator = ";";
    public TwinBuffer Buffer;
    Socket socket = null;

    private Pose groundAnchorPose;
    private Pose cameraPose;


    int messageCounter; //Zählt die Anzahl der erhaltenen Messages
    public boolean stillConnected;

    private void resetAllMessages(String Message) {
        if (Message.indexOf(recieveResetHeader) != 0)
            messageCounter = 0;
    }

    private String formConfigurationMessage() {

        Log.e(TAG, "Server formCingurationMessage called");
        String message = "";

        message = sendCFGHeader +
                Build.MODEL + seperator +//devicename
                Resources.getSystem().getDisplayMetrics().widthPixels + seperator +// screen width
                Resources.getSystem().getDisplayMetrics().heightPixels + seperator +// screen heigth
                50 + seperator;// divider
        return message;
    }

    private String formCamMatriceMessage(Pose camPose, Pose anchorPose) {
        Log.e(TAG, "Server formCamMatriceMessage called");
        String message = sendCAMHeader;
        float mat4_4[] = new float[16];
        camPose.toMatrix(mat4_4, 0);

        for (int i = 0; i < 16; i++) {
            message += (mat4_4[i] + seperator);
        }

        return message;
    }

    public Server(Context context, IPackageRecivedCallback packageRecipient) {
        Log.d("Server constructed", "Server Constructor reached");
        Buffer = new TwinBuffer(context, this);
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
            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            Log.d("Server::run", "Server Listening reached");

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket, packageRecipient);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            Log.d("Server run ", "Server run interrupted");
        }
    }

    //Writes Data to the socket
    class CommunicationThread implements Runnable {

        private Socket clientSocket;
        private BufferedWriter output;
        private BufferedReader input;
        IPackageRecivedCallback packageRecipient;

        //Senden
        public CommunicationThread(Socket clientSocket, IPackageRecivedCallback packageRecipient) {
            Log.d("Server::run", "Server Listening reached");


            this.packageRecipient = packageRecipient;
            this.clientSocket = clientSocket;

        }

        public void run() {
            //Senden
            try {
                this.output = new BufferedWriter(new OutputStreamWriter(System.out));


            } catch (Exception e) {
                e.printStackTrace();
            }


            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
            //process Input
            try {

                String read = input.readLine();
                //TODO Remove Debug String
                Log.d(this.getClass().getSimpleName(), "Recived Data::" + read);

                //Setzt die Verbindung zurück wenn der Host das anfordert
                resetAllMessages(read);

                if (read.contains(recieveDataHeader)) {
                    read.replace(recieveDataHeader, "");
                    Bitmap refToWriteBuffer = Buffer.getWriteBuffer();
                    //noinspection UnusedAssignment
                    refToWriteBuffer = new BitmapFactory().decodeStream(this.clientSocket.getInputStream());
                    Buffer.switchBuffer();
                    packageRecipient.callback(Buffer.getDrawBuffer());
                }
                //write Output
                output.write("This will be printed on stdout!\n");
                output.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        }
    }
}

