package com.google.ar.core.examples.app.common.tcpClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.content.Context;
import android.util.Log;
import com.google.ar.core.examples.app.common.helpers.comonUtils;

public class Server {
    //Size of the Transfered Datagrams
    int MAX_UDP_DATAGRAM_RCV_LEN = 1024;
    int MAX_UDP_DATAGRAM_SND_LEN = 1024;
    //Server Port
    int UDP_SERVER_PORT = 8090;

    //Message Zähler - 0 bedeutet die Verbindung wurde reinitialisiert
    public int messageCounter = 0;

    //Basically Watchdogvariable- every arriving Datagram resets the timer
    public  boolean stillConnected = true;

    public DatagramReciever datagramReciever = null;

    //Callback to return recieved data to the corresponding thread
    IPackageRecivedCallback packageRecipient;
    Context context;

    //Constructor
    public Server(Context context, IPackageRecivedCallback packageRecipient) {
        this.packageRecipient = packageRecipient;
        this.context = context;
    }

    //Restart the Server on Resume
    protected void onResume() {
        datagramReciever = new DatagramReciever();
        datagramReciever.start();
    }

    protected void onPause() {
        datagramReciever.kill();
    }

    public class DatagramReciever extends Thread {
        boolean bKeepRunning = true;
        private String datagramToSend = "";
        private boolean newDatagramToSend = false;
        int TIME_OF_FRAME_IN_MS = 30;
        DatagramSocket socket = null;

        //get him, he did not write a getter for a private variable
        private int writeBuffer = 0;

        int getReadBuffer() {
            if (writeBuffer == 1) return 0;
            return 1;
        }

        final byte[] searchDataHeaderByte = "SPRINGARSND;DATA=".getBytes();
        final byte[] searchResetHeaderByte ="SPRINGARSND;RESET=".getBytes();

        private void handleManagedTraffic(byte[] payload) {
            if (comonUtils.indexOf(payload, searchResetHeaderByte) != -1)  messageCounter = 0;
        }




        private boolean isDataMessage(byte [] payload) {
            return -1  != comonUtils.indexOf(payload, searchDataHeaderByte);
        }

        void switchBuffer() {
            writeBuffer = getReadBuffer();
        }

        public void run() {
            String message;
            byte[][] rcv_message = new byte[2][MAX_UDP_DATAGRAM_RCV_LEN];
            byte[] snd_message = new byte[MAX_UDP_DATAGRAM_SND_LEN];

            DatagramPacket snd_packet = new DatagramPacket(snd_message, snd_message.length);


            //Initialisation
            try {
                DatagramSocket socket = new DatagramSocket(UDP_SERVER_PORT);
                while (bKeepRunning) {
                   
                    DatagramPacket rcv_packet = new DatagramPacket(rcv_message[writeBuffer], rcv_message.length);

                    // Receiving
                    socket.setSoTimeout(TIME_OF_FRAME_IN_MS);
                    socket.receive(rcv_packet);

                    //TODO Delete String conversion
                    message = new String(rcv_message[writeBuffer], 0, rcv_message[writeBuffer].length);
                    Log.d("altServer::rcv", message);

                    // callback
                    if (isDataMessage(rcv_message[getReadBuffer()])) {
                        packageRecipient.callback(rcv_message[getReadBuffer()], rcv_message[getReadBuffer()].length);
                    } else {
                        handleManagedTraffic(rcv_message[getReadBuffer()]);
                    }

                    switchBuffer();
                    //callback to update Buffer

                    if (newDatagramToSend) {
                        snd_message = datagramToSend.getBytes();
                        snd_packet = new DatagramPacket(snd_message, snd_message.length, snd_packet.getAddress(), snd_packet.getPort());
                        socket.send(snd_packet);
                        messageCounter++;
                        Log.d("altServer::snd", datagramToSend);
                        newDatagramToSend = false;
                    }
                    //Sending
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (socket != null) {
                socket.close();
            }
        }



        public void kill() {
            bKeepRunning = false;
        }

        public boolean setSendToSpringMessage(String toSend) {
            if (newDatagramToSend) return false;

            datagramToSend = toSend;
            newDatagramToSend = true;
            return newDatagramToSend;
        }
    }
}