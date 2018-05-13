package com.google.ar.core.examples.java.common.tcpClient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class Server {

    private ServerSocket serverSocket;
    IPackageRecivedCallback packageRecipient;
    Handler updateConversationHandler;

    Thread serverThread = null;
    String pngSizeStartTokken = "SerializedSize:";
    String pngSizeEndTokken = ";";

    private TextView text;
    char[] deserialize;

    public static final int SERVERPORT = 647;


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

    class ServerThread implements Runnable {
        ServerThread() {

        }

        ;


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

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;
        IPackageRecivedCallback packageRecipient;

        public CommunicationThread(Socket clientSocket, IPackageRecivedCallback packageRecipient) {
            this.packageRecipient = packageRecipient;
            this.clientSocket = clientSocket;

            try {

                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) try {

                String read = input.readLine();
                Log.d(this.getClass().getSimpleName(), read);

                if (read.contains(pngSizeStartTokken)) {
                    int sizeOfPng = Integer.parseInt(read.substring(read.indexOf(pngSizeStartTokken) + pngSizeStartTokken.length(), read.indexOf(pngSizeEndTokken)));


                    try {
                        packageRecipient.callback(new BitmapFactory().decodeStream( this.clientSocket.getInputStream()));
                    } catch (IOException ex) {
                        System.out.println("Can't get socket input stream. ");
                    }





                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


}

